<#
    demarrer_all.ps1
    ---------------------------------------------------------------------------
    Orchestre le lancement du Back-End (Spring Boot) et du Front-End (Angular)
    dans deux consoles distinctes :

      * si l'une des deux consoles se ferme  -> tout est arrete ;
      * si ce script (donc demarrer_all.bat) est arrete -> les deux consoles
        sont arretees.

    Le mecanisme utilise un "Job Object" Windows avec le drapeau
    JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE : des que ce processus se termine (fin
    normale, fermeture de la fenetre ou kill), Windows tue automatiquement tous
    les processus rattaches au job, y compris leurs descendants (java, node).
#>

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$root        = $PSScriptRoot
$backendDir  = Join-Path $root 'DEVELOPPEMENT\Back-End\service-connectmarket'
$frontendDir = Join-Path $root 'DEVELOPPEMENT\Front-End'

if (-not (Test-Path -LiteralPath (Join-Path $backendDir  'mvnw.cmd'))) {
    throw "Back-End introuvable (mvnw.cmd manquant) : $backendDir"
}
if (-not (Test-Path -LiteralPath (Join-Path $frontendDir 'package.json'))) {
    throw "Front-End introuvable (package.json manquant) : $frontendDir"
}

# ---------------------------------------------------------------------------
# 1. Declarations Win32 pour le Job Object.
# ---------------------------------------------------------------------------
$jobSource = @'
using System;
using System.Runtime.InteropServices;

public static class NativeJob
{
    [StructLayout(LayoutKind.Sequential)]
    public struct IO_COUNTERS
    {
        public ulong ReadOperationCount;
        public ulong WriteOperationCount;
        public ulong OtherOperationCount;
        public ulong ReadTransferCount;
        public ulong WriteTransferCount;
        public ulong OtherTransferCount;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct JOBOBJECT_BASIC_LIMIT_INFORMATION
    {
        public long PerProcessUserTimeLimit;
        public long PerJobUserTimeLimit;
        public uint LimitFlags;
        public UIntPtr MinimumWorkingSetSize;
        public UIntPtr MaximumWorkingSetSize;
        public uint ActiveProcessLimit;
        public UIntPtr Affinity;
        public uint PriorityClass;
        public uint SchedulingClass;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct JOBOBJECT_EXTENDED_LIMIT_INFORMATION
    {
        public JOBOBJECT_BASIC_LIMIT_INFORMATION BasicLimitInformation;
        public IO_COUNTERS IoInfo;
        public UIntPtr ProcessMemoryLimit;
        public UIntPtr JobMemoryLimit;
        public UIntPtr PeakProcessMemoryUsed;
        public UIntPtr PeakJobMemoryUsed;
    }

    const int  JobObjectExtendedLimitInformation = 9;
    const uint JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x2000;

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    static extern IntPtr CreateJobObject(IntPtr lpJobAttributes, string lpName);

    [DllImport("kernel32.dll", SetLastError = true)]
    static extern bool SetInformationJobObject(IntPtr hJob, int infoClass, IntPtr info, uint infoLength);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool AssignProcessToJobObject(IntPtr hJob, IntPtr hProcess);

    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern bool CloseHandle(IntPtr hObject);

    public static IntPtr CreateKillOnCloseJob()
    {
        IntPtr hJob = CreateJobObject(IntPtr.Zero, null);
        if (hJob == IntPtr.Zero)
            throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error());

        var info = new JOBOBJECT_EXTENDED_LIMIT_INFORMATION();
        info.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;

        int size = Marshal.SizeOf(typeof(JOBOBJECT_EXTENDED_LIMIT_INFORMATION));
        IntPtr ptr = Marshal.AllocHGlobal(size);
        try
        {
            Marshal.StructureToPtr(info, ptr, false);
            if (!SetInformationJobObject(hJob, JobObjectExtendedLimitInformation, ptr, (uint)size))
                throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error());
        }
        finally
        {
            Marshal.FreeHGlobal(ptr);
        }
        return hJob;
    }
}
'@

Add-Type -TypeDefinition $jobSource -Language CSharp
$job = [NativeJob]::CreateKillOnCloseJob()

# PID du processus parent (cmd.exe de demarrer_all.bat) : permet de tout
# arreter si le script maitre disparait sans avoir pu executer de nettoyage.
$parentPid = 0
try {
    $parentPid = (Get-CimInstance Win32_Process -Filter "ProcessId = $PID").ParentProcessId
} catch { }

# ---------------------------------------------------------------------------
# 2. Lancement d'une application dans sa propre console.
# ---------------------------------------------------------------------------
function Start-AppConsole {
    param(
        [Parameter(Mandatory)][string]$Title,
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [Parameter(Mandatory)][string]$CommandLine
    )

    # /c : la console vit aussi longtemps que la commande, puis se ferme.
    $inner = 'title ' + $Title + ' & ' + $CommandLine
    $proc  = Start-Process -FilePath 'cmd.exe' `
                          -ArgumentList '/c', ('"' + $inner + '"') `
                          -WorkingDirectory $WorkingDirectory `
                          -PassThru

    # Toute la descendance (java, node...) herite du job et sera tuee avec lui.
    if (-not [NativeJob]::AssignProcessToJobObject($job, $proc.Handle)) {
        Write-Warning "Impossible de rattacher « $Title » au job (code $([Runtime.InteropServices.Marshal]::GetLastWin32Error()))."
    }
    return $proc
}

$processes = @()
$stoppedBy = $null

try {
    Write-Host 'Lancement du Back-End  (Spring Boot)...' -ForegroundColor Cyan
    $processes += [pscustomobject]@{
        Name    = 'Back-End'
        Process = Start-AppConsole -Title 'CLAUDIA - Back-End' `
                                   -WorkingDirectory $backendDir `
                                   -CommandLine 'mvnw.cmd spring-boot:run'
    }

    Write-Host 'Lancement du Front-End (Angular)...' -ForegroundColor Cyan
    $processes += [pscustomobject]@{
        Name    = 'Front-End'
        Process = Start-AppConsole -Title 'CLAUDIA - Front-End' `
                                   -WorkingDirectory $frontendDir `
                                   -CommandLine 'npm start'
    }

    Write-Host ''
    Write-Host 'Tout est lance. Fermez une console (ou cette fenetre) pour tout arreter.' -ForegroundColor Green
    Write-Host ''

    while ($true) {
        Start-Sleep -Milliseconds 400

        foreach ($app in $processes) {
            if ($app.Process.HasExited) { $stoppedBy = $app.Name; break }
        }
        if ($stoppedBy) { break }

        if ($parentPid -and -not (Get-Process -Id $parentPid -ErrorAction SilentlyContinue)) {
            $stoppedBy = 'demarrer_all.bat'
            break
        }
    }
}
finally {
    # Nettoyage : on tue l'arbre de chaque application, puis on ferme le job
    # (ce qui elimine tout eventuel processus restant).
    foreach ($app in $processes) {
        try {
            if ($app.Process -and -not $app.Process.HasExited) {
                & taskkill.exe /PID $app.Process.Id /T /F 2>$null | Out-Null
            }
        } catch { }
    }
    [NativeJob]::CloseHandle($job) | Out-Null
}

if ($stoppedBy) {
    Write-Host "Arret detecte ($stoppedBy) -> fermeture des deux consoles." -ForegroundColor Yellow
}
