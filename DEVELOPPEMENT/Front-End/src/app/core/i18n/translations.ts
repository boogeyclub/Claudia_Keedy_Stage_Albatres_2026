export type TranslationNode = string | { [key: string]: TranslationNode };
export type TranslationDictionary = Record<string, TranslationNode>;

export const TRANSLATIONS = {
  en: {
    meta: {
      landingTitle: 'CacaoMarketCM | Cocoa trade with confidence',
      loginTitle: 'CacaoMarketCM | Sign in',
      registrationTitle: 'CacaoMarketCM | Create your account',
      confirmationTitle: 'CacaoMarketCM | Confirm your account',
      administratorDashboardTitle: 'CacaoMarketCM | Administrator workspace',
      sellerDashboardTitle: 'CacaoMarketCM | Seller workspace',
      userDashboardTitle: 'CacaoMarketCM | User workspace',
      accountTitle: 'CacaoMarketCM | Account settings',
      passwordResetRequestTitle: 'CacaoMarketCM | Reset your password',
      passwordResetConfirmationTitle: 'CacaoMarketCM | Choose a new password'
    },
    common: {
      brandName: 'CacaoMarketCM',
      homeAria: 'CacaoMarketCM home',
      primaryNavigation: 'Primary navigation',
      languageSelector: 'Choose language',
      switchToLanguage: 'Switch to {{language}}',
      languageNames: {
        english: 'English',
        french: 'French'
      },
      languageCodes: {
        english: 'EN',
        french: 'FR'
      }
    },
    notifications: {
      outletAria: 'Notifications',
      dismiss: 'Dismiss notification',
      types: {
        loading: 'In progress',
        success: 'Success',
        error: 'Error',
        warning: 'Attention',
        info: 'Information'
      },
      forms: {
        invalid: 'Review the highlighted fields before continuing.'
      },
      api: {
        requestFailed: 'We could not complete the request. Please try again.'
      },
      admin: {
        loadFailed: 'We could not load the protected table records right now.',
        connectionFailed: 'The browser could not reach the configured API. Check that the backend is running, the /cacaomarketcm context is used, and config.json or CORS is configured correctly.',
        sessionExpired: 'Your administrator session is no longer active. Sign in again, then retry.',
        accessDenied: 'Your active account is not allowed to read administrator table records.',
        backendDataFailed: 'The API was reached but could not complete the protected table operation. Verify the database connection and that the current gu schema has been applied.',
        backendDataFailedWithRequestId: 'The API was reached but could not complete the protected table operation. Verify the database and inspect the backend log for request ID {{requestId}}.',
        lookupFailed: 'We could not load one of the safe lookup lists. Refresh and try again.',
        saving: 'Saving the protected table change…',
        saved: 'The protected table change was completed.',
        saveFailed: 'We could not save this protected table change. Review the values and try again.',
        removing: 'Completing the protected action…',
        removed: 'The protected action was completed.',
        removeFailed: 'We could not complete this protected action. Refresh and try again.'
      },
      registration: {
        starting: 'Creating your account…',
        success: 'Your registration request was accepted. Check your email to confirm it.'
      },
      confirmation: {
        starting: 'Confirming your CacaoMarketCM account…',
        success: 'Your account is confirmed. Redirecting you to sign in…',
        alreadyConfirmed: 'This account was already confirmed. Redirecting you to sign in…',
        missingToken: 'This confirmation link is incomplete. Please use the complete link from your email.'
      },
      login: {
        starting: 'Checking your credentials…',
        success: 'You are signed in.'
      },
      passwordReset: {
        requesting: 'Preparing your secure password reset link…',
        requestAccepted: 'If your confirmed account uses this email address, a reset link is on its way.',
        completing: 'Updating your password securely…',
        completed: 'Your password has been reset. Redirecting you to sign in…'
      },
      session: {
        signingOut: 'Signing you out securely…',
        signedOut: 'You have been signed out.',
        signOutFailed: 'We could not sign you out. Please try again.',
        disconnecting: 'Disconnecting the selected browser session…',
        disconnected: 'The browser session was disconnected.',
        disconnectFailed: 'We could not disconnect that browser session. Please try again.',
        sessionsLoadFailed: 'We could not load your browser sessions right now.'
      },
      language: {
        changed: 'Language changed to {{language}}.'
      }
    },
    landing: {
      brandTagline: 'Trade with intent',
      navigation: {
        howItWorks: 'How it works',
        forFarmers: 'For farmers',
        forBuyers: 'For buyers',
        whyCacaoMarketCM: 'Why CacaoMarketCM'
      },
      actions: {
        joinMarket: 'Join the market',
        listBatch: 'List a cocoa batch',
        sourceCocoa: 'Source cocoa with intent',
        seeWhereYouFit: 'See where you fit',
        growCocoa: 'I grow cocoa',
        sourceCocoaShort: 'I source cocoa',
        signIn: 'Sign in'
      },
      hero: {
        badge: 'Built for serious cocoa trade',
        titleStart: 'Where cocoa harvests meet',
        titleAccent: 'serious demand.',
        description: 'CacaoMarketCM helps farmers present large, market-ready cocoa lots and helps committed buyers find the volumes, origin, and timing they need.',
        largeBatchFocus: 'Large-batch focus',
        betterBriefings: 'Better briefings',
        directDialogue: 'Direct dialogue',
        imageAlt: 'Cocoa producers preparing large sacks of cocoa at a collection point',
        marketReadyLot: 'Market-ready lot',
        cocoaInVolume: 'Cocoa, in volume',
        clearerStartingPoint: 'A clearer starting point',
        fromHarvestToMarket: 'From harvest to market',
        harvestMarketDescription: 'Make availability and commercial intent visible.',
        tradeBeginsWith: 'Trade begins with',
        volumeAndVisibility: 'volume + visibility'
      },
      highlights: {
        lotBasedTitle: 'Lot-based offers',
        lotBasedDescription: 'Built around commercial volumes.',
        discoveryTitle: 'Purposeful discovery',
        discoveryDescription: 'For growers and buyers with intent.',
        firstStepTitle: 'A simpler first step',
        firstStepDescription: 'Share the right facts from day one.'
      },
      value: {
        eyebrow: 'A marketplace with a purpose',
        title: 'Less noise. Better cocoa conversations.',
        description: 'CacaoMarketCM is designed around the information that makes a bulk cocoa conversation worth having — before time is spent on either side.',
        designedForTrade: 'Designed for the trade',
        advantages: {
          volume: {
            title: 'Made for volume',
            description: 'Present harvests as lots so every conversation starts with the scale that matters.'
          },
          signals: {
            title: 'Clear market signals',
            description: 'Share the facts buyers need early: origin, readiness, quantity, and availability.'
          },
          matches: {
            title: 'More purposeful matches',
            description: 'Give farmers and buyers a focused place to start commercially meaningful conversations.'
          }
        }
      },
      process: {
        eyebrow: 'How it works',
        title: 'A better route from cocoa lot to commercial conversation.',
        description: 'CacaoMarketCM gives each side a structured way to show what they have and what they are looking for.',
        steps: {
          publish: {
            title: 'Publish your cocoa lot',
            description: 'Describe your available volume, quality, location, and preferred delivery window in one clear market brief.'
          },
          demand: {
            title: 'Meet serious demand',
            description: 'Reach buyers looking for commercial quantities, not one-off samples or unclear conversations.'
          },
          clarity: {
            title: 'Move forward with clarity',
            description: 'Compare interest, discuss terms, and turn a qualified connection into a confident next step.'
          }
        }
      },
      farmers: {
        eyebrow: 'For farmers & cooperatives',
        title: 'Make your harvest easier to understand — and harder to overlook.',
        description: 'Create a clear listing around volume, origin, quality details, and readiness. Let the right buyers see the opportunity in your batch.',
        action: 'I have cocoa to sell'
      },
      buyers: {
        eyebrow: 'For buyers & sourcing teams',
        title: 'Start with lots that match the size of your ambition.',
        description: 'Find producers ready to discuss commercial quantities, then open a direct conversation with the context your sourcing team needs.',
        action: 'I am looking to buy'
      },
      finalCta: {
        eyebrow: 'A more connected cocoa market',
        title: 'Bring your next cocoa conversation into focus.',
        description: 'Whether you are preparing a major lot or sourcing one, CacaoMarketCM gives you a more intentional place to begin.'
      },
      footer: {
        description: 'Helping cocoa producers and buyers find a clearer path to trade.',
        farmers: 'Farmers',
        buyers: 'Buyers',
        about: 'About'
      }
    },
    auth: {
      shared: {
        secureAccess: 'Secure access for the cocoa market',
        backToMarket: 'Back to CacaoMarketCM',
        footer: 'CacaoMarketCM — cocoa trade with confidence',
        profileProtection: 'Your account information is protected and used only to create your market profile.',
        showPassword: 'Show password',
        hidePassword: 'Hide password'
      },
      login: {
        eyebrow: 'Welcome back',
        title: 'Sign in to move cocoa trade forward.',
        description: 'Access your CacaoMarketCM workspace to manage your profile, market activity, and conversations.',
        identityLabel: 'Email address or login',
        identityPlaceholder: 'you@example.com or your login',
        passwordLabel: 'Password',
        passwordPlaceholder: 'Enter your password',
        rememberMe: 'Keep this session active longer',
        forgotPassword: 'Forgot password?',
        submit: 'Sign in securely',
        submitting: 'Signing in…',
        signedIn: 'You are signed in as {{name}}.',
        errors: {
          invalidCredentials: 'Your email, login, or password is incorrect.',
          pendingConfirmation: 'Confirm the email sent during registration before signing in.',
          requestFailed: 'We could not sign you in right now. Please try again.'
        },
        noAccount: 'New to CacaoMarketCM?',
        createAccount: 'Create an account',
        identityRequired: 'Enter your email address or login.',
        passwordRequired: 'Enter your password.',
        passwordMin: 'Your password must contain at least 8 characters.'
      },
      passwordReset: {
        request: {
          eyebrow: 'Password recovery',
          title: 'Reset your password securely.',
          description: 'Enter the email address of your confirmed CacaoMarketCM account. If it is eligible, we will send a single-use reset link.',
          emailLabel: 'Account email address',
          emailPlaceholder: 'you@example.com',
          emailRequired: 'Enter your email address.',
          emailInvalid: 'Enter a valid email address.',
          submit: 'Send reset link',
          submitting: 'Sending secure link…',
          successTitle: 'Check your email inbox.',
          successDescription: 'For security, we only send a link when this email belongs to a confirmed CacaoMarketCM account. Check the email for its expiry time.',
          backToSignIn: 'Back to sign in',
          errors: {
            deliveryUnavailable: 'We could not send a reset email right now. Please try again later.',
            requestFailed: 'We could not start your password reset request. Please try again.'
          }
        },
        confirmation: {
          eyebrow: 'Choose a new password',
          title: 'Create a fresh, secure password.',
          description: 'This personal reset link can be used once. Resetting your password signs this account out on every browser.',
          missingToken: 'This reset link is incomplete. Open the complete link from your email or request a new one.',
          passwordLabel: 'New password',
          passwordPlaceholder: 'At least 8 characters',
          confirmPasswordLabel: 'Confirm new password',
          confirmPasswordPlaceholder: 'Repeat your new password',
          passwordRequired: 'Create a new password.',
          passwordMin: 'Your new password must contain at least 8 characters.',
          confirmPasswordRequired: 'Confirm your new password.',
          passwordMismatch: 'The password confirmation does not match.',
          submit: 'Reset password',
          submitting: 'Resetting password…',
          loadingTitle: 'Checking your secure reset link…',
          successTitle: 'Your password was reset.',
          successDescription: 'All browser sessions for this account have been signed out. You can now sign in with your new password.',
          redirecting: 'Redirecting to sign in in {{seconds}} seconds.',
          signInNow: 'Sign in now',
          requestNewLink: 'Request a new reset link',
          errorTitle: 'We could not reset your password.',
          errors: {
            invalid: 'This reset link is invalid. Request a new link and use the newest email.',
            expired: 'This reset link has expired. Request a new one.',
            used: 'This reset link was already used. Request a new one if you still need help.',
            unavailable: 'This account is not available for password reset.',
            passwordInvalid: 'This password is not supported. Use at most 72 characters.',
            requestFailed: 'We could not reset your password right now. Please try again.'
          }
        }
      },
      confirmation: {
        eyebrow: 'Email verification',
        title: 'Confirming your CacaoMarketCM account',
        description: 'We are securely verifying your personal confirmation link.',
        loadingTitle: 'Confirming your email address…',
        loadingDescription: 'Please keep this page open while we activate your account.',
        successTitle: 'Your account is confirmed.',
        successDescription: 'You can now sign in and begin using CacaoMarketCM.',
        redirecting: 'Redirecting to sign in in {{seconds}} seconds.',
        signInNow: 'Sign in now',
        errorTitle: 'We could not confirm your account.',
        missingToken: 'This confirmation link is incomplete. Open the complete link from your email or register again.',
        registerAgain: 'Create a new account',
        errors: {
          invalid: 'This confirmation link is invalid. Please request a new registration link.',
          expired: 'This confirmation link has expired. Please register again.',
          alreadyConfirmed: 'This account has already been confirmed. You can sign in now.',
          unavailable: 'This confirmation link is not available for activation. Please register again.',
          requestFailed: 'We could not confirm your account right now. Please try again.'
        }
      },
      registration: {
        eyebrow: 'Create your market profile',
        title: 'Start your next cocoa trade with clarity.',
        description: 'Create an account to present your interest in selling or sourcing commercial cocoa lots.',
        chooseRole: 'How will you use CacaoMarketCM?',
        sellerTitle: 'I want to sell cocoa',
        sellerDescription: 'For farmers, cooperatives, and sellers with cocoa lots to present.',
        buyerTitle: 'I want to source cocoa',
        buyerDescription: 'For buyers and sourcing teams looking for commercial quantities.',
        clientProfileTitle: 'How are you sourcing cocoa?',
        clientProfileDescription: 'Choose the legal profile that will hold this buyer account.',
        individualTitle: 'Private individual',
        individualDescription: 'I am sourcing cocoa in my own name.',
        enterpriseTitle: 'Company',
        enterpriseDescription: 'I am sourcing cocoa for a registered enterprise.',
        clientProfileRequired: 'Choose whether this buyer account is for a private individual or a company.',
        enterpriseDetailsTitle: 'Company identification',
        enterpriseDetailsDescription: 'Enter the legal business details used for this buyer account.',
        companyNameLabel: 'Registered company name',
        companyNamePlaceholder: 'Your company’s legal name',
        companyNameRequired: 'Enter the registered company name.',
        niuLabel: 'NIU',
        niuPlaceholder: 'Unique identification number',
        niuRequired: 'Enter the company NIU.',
        rccmLabel: 'RCCM',
        rccmPlaceholder: 'Trade and personal property credit register number',
        rccmRequired: 'Enter the company RCCM.',
        representativeNotice: 'Your first and last name below identify the enterprise’s legal representative or primary contact.',
        representativeFirstNameLabel: 'Legal representative’s first name',
        representativeFirstNamePlaceholder: 'Representative’s first name',
        representativeFirstNameRequired: 'Enter the legal representative’s first name.',
        representativeLastNameLabel: 'Legal representative’s last name',
        representativeLastNamePlaceholder: 'Representative’s last name',
        representativeLastNameRequired: 'Enter the legal representative’s last name.',
        firstNameLabel: 'First name',
        firstNamePlaceholder: 'Your first name',
        lastNameLabel: 'Last name',
        lastNamePlaceholder: 'Your last name',
        emailLabel: 'Email address',
        emailPlaceholder: 'you@example.com',
        loginLabel: 'Choose a login',
        loginPlaceholder: 'Your preferred login',
        passwordLabel: 'Create a password',
        passwordPlaceholder: 'At least 8 characters',
        confirmPasswordLabel: 'Confirm password',
        confirmPasswordPlaceholder: 'Repeat your password',
        agreement: 'I agree to create a CacaoMarketCM account for market-related communication.',
        submit: 'Create my account',
        submitting: 'Sending confirmation email…',
        confirmationSent: 'A confirmation email was sent to {{email}}.',
        confirmationExpiry: 'Open its personal link within 3 hours. Unconfirmed registrations expire and are removed automatically.',
        errors: {
          identityExists: 'An account already uses this email address or login.',
          enterpriseIdentifierExists: 'An account already uses one of these enterprise identifiers.',
          identityOrEnterpriseIdentifierExists: 'An account already uses this email address, login, or enterprise identifier.',
          clientProfileInvalid: 'Review the buyer profile and enterprise details, then try again.',
          deliveryUnavailable: 'We could not send your confirmation email. Please try again later.',
          requestFailed: 'We could not start your registration. Please review your information and try again.'
        },
        alreadyAccount: 'Already have an account?',
        signIn: 'Sign in',
        roleRequired: 'Choose how you will use CacaoMarketCM.',
        firstNameRequired: 'Enter your first name.',
        lastNameRequired: 'Enter your last name.',
        emailRequired: 'Enter your email address.',
        emailInvalid: 'Enter a valid email address.',
        loginRequired: 'Choose a login.',
        loginMin: 'Your login must contain at least 3 characters.',
        passwordRequired: 'Create a password.',
        passwordMin: 'Your password must contain at least 8 characters.',
        confirmPasswordRequired: 'Confirm your password.',
        passwordMismatch: 'The password confirmation does not match.',
        agreementRequired: 'Please confirm your agreement to continue.'
      }
    },
    dashboard: {
      roles: {
        administrator: 'Administrator',
        seller: 'Seller',
        user: 'User'
      },
      header: {
        overview: 'Overview',
        accountSettings: 'Account settings',
        signOut: 'Sign out',
        accountMenu: 'Account menu',
        signedInAs: 'Signed in as {{role}}',
        openAccountMenu: 'Open account menu'
      },
      shared: {
        workspace: 'CacaoMarketCM workspace',
        ready: 'Ready for your next step',
        nextStep: 'Suggested next step',
        viewAccount: 'View account settings',
        secureWorkspace: 'Your workspace is protected by a browser session.'
      },
      admin: {
        eyebrow: 'Administrator workspace',
        title: 'Keep the CacaoMarketCM ecosystem moving with confidence.',
        description: 'Your operational home for supervising access, trade activity, and the platform experience.',
        accessCard: {
          title: 'Access governance',
          description: 'Review roles and keep the right people connected to the marketplace.'
        },
        sessionsCard: {
          title: 'Connected browsers',
          description: 'Session controls make it easier to keep account access deliberate and secure.'
        },
        marketCard: {
          title: 'Market readiness',
          description: 'Prepare the operating view for the next wave of cocoa activity.'
        },
        nextTitle: 'Set the operational rhythm',
        nextDescription: 'Your administrator dashboard is ready for user management, role controls, and future market oversight modules.',
        accountAction: 'Open my account settings',
        tableManagement: {
          eyebrow: 'Schema controls',
          title: 'Manage the protected gu data tables',
          description: 'Open a purpose-built management screen for each schema table. Security records stay auditable and use only narrowly scoped safe actions.',
          tablesAvailable: 'tables available',
          manage: 'Manage table',
          audit: 'Audit safe'
        },
        tables: {
          userTypes: {
            title: 'User types',
            description: 'Maintain the role catalogue used by accounts.',
            securityNote: 'Built-in CLIENT, VENDEUR, and ADMINISTRATEUR roles are protected from deletion and unsafe code changes.'
          },
          users: {
            title: 'Users',
            description: 'Create, update, suspend, or remove application accounts.',
            securityNote: 'Passwords are accepted only when creating a controlled account and are never shown, edited, or returned by this screen. CLIENT accounts are created through the registration workflow so their legal profile is always recorded.'
          },
          individualClients: {
            title: 'Private buyer profiles',
            description: 'Audit CLIENT accounts registered as private individuals.',
            securityNote: 'This is an audit view of the profile relationship. A private buyer type is chosen during secure registration and cannot be removed independently from the account.'
          },
          enterpriseClients: {
            title: 'Enterprise buyer profiles',
            description: 'Review and correct the legal details of registered enterprise buyers.',
            securityNote: 'Only the company details are editable here. NIU and RCCM remain unique, and a profile type cannot be changed independently from its CLIENT account.'
          },
          sessions: {
            title: 'Browser sessions',
            description: 'Audit signed-in browsers and revoke access when needed.',
            securityNote: 'This is an audit and revocation view. Session hashes and browser cookies are never displayed.'
          },
          confirmations: {
            title: 'Registration confirmations',
            description: 'Audit account-confirmation lifecycle records.',
            securityNote: 'This is an audit view. Confirmation tokens and token hashes are never displayed; only an unconfirmed pending registration can be cancelled.'
          },
          passwordResets: {
            title: 'Password reset requests',
            description: 'Audit reset-link lifecycle records and revoke unused links.',
            securityNote: 'This is an audit and revocation view. Reset tokens and token hashes are never displayed.'
          },
          basicRights: {
            title: 'Basic rights',
            description: 'Maintain named application capabilities.',
            securityNote: 'The required APP-CONN capability remains protected. Rights are assigned to types from the separate assignment table.'
          },
          rightAssignments: {
            title: 'Type-to-right assignments',
            description: 'Grant a basic right to a user type or remove a safe assignment.',
            securityNote: 'Administrator assignments are protected by the database and cannot be removed from this screen.'
          },
          passwordHistory: {
            title: 'Password history',
            description: 'Audit password lifecycle metadata without security material.',
            securityNote: 'This table is strictly read-only. Password values and password hashes are never displayed or editable.'
          }
        },
        management: {
          eyebrow: 'Administrator table management',
          auditOnly: 'Audit-safe view',
          back: 'Back to administrator dashboard',
          createEyebrow: 'New controlled record',
          editEyebrow: 'Controlled update',
          createTitle: 'Create a record',
          editTitle: 'Update a record',
          temporaryPasswordNotice: 'The temporary password is sent only to the secure server for one-time hashing. It is never displayed again.',
          selectOption: 'Select an option',
          invalidField: 'Enter a valid value for this field.',
          confirmActionTitle: 'Confirm protected action',
          confirmActionDescription: 'Confirm this action for record #{{id}}. This action is logged and may revoke access immediately.',
          recordsTitle: 'Safe record view',
          recordsDescription: 'Only approved metadata fields are visible here.',
          recordsCount: 'records',
          loading: 'Loading protected records…',
          loadError: 'Protected records could not be loaded right now.',
          noRecords: 'No approved records are available for this table.'
        },
        actions: {
          add: 'Add record',
          refresh: 'Refresh',
          edit: 'Edit',
          delete: 'Delete',
          revoke: 'Revoke session',
          cancelPending: 'Cancel registration',
          revokeReset: 'Revoke reset link',
          removeAssignment: 'Remove assignment',
          cancel: 'Cancel',
          createRecord: 'Create record',
          saveChanges: 'Save changes',
          tryAgain: 'Try again'
        },
        columns: {
          id: 'ID',
          code: 'Code',
          name: 'Name',
          firstName: 'First name',
          lastName: 'Last name',
          email: 'Email',
          login: 'Login',
          userType: 'User type',
          status: 'Status',
          createdAt: 'Created',
          browser: 'Browser',
          extended: 'Extended',
          lastSeenAt: 'Last active',
          expiresAt: 'Expires',
          revokedAt: 'Revoked',
          confirmedAt: 'Confirmed',
          usedAt: 'Used',
          basicRight: 'Basic right',
          currentPassword: 'Current',
          companyName: 'Company name',
          niu: 'NIU',
          rccm: 'RCCM',
          representativeFirstName: 'Representative first name',
          representativeLastName: 'Representative last name',
          recordedAt: 'Recorded',
          changedAt: 'Changed',
          actions: 'Actions'
        },
        fields: {
          code: 'Code',
          name: 'Name',
          userType: 'User type',
          firstName: 'First name',
          lastName: 'Last name',
          email: 'Email address',
          login: 'Login',
          status: 'Status',
          temporaryPassword: 'Temporary password',
          basicRight: 'Basic right',
          companyName: 'Registered company name',
          niu: 'NIU',
          rccm: 'RCCM'
        },
        statuses: {
          active: 'Active',
          suspended: 'Suspended',
          pending: 'Pending confirmation'
        },
        values: {
          yes: 'Yes',
          no: 'No'
        }
      },
      seller: {
        eyebrow: 'Seller workspace',
        title: 'Turn your cocoa availability into a clear market story.',
        description: 'This is your home for preparing lots, presenting readiness, and following buyer interest.',
        prepareCard: {
          title: 'Prepare your next lot',
          description: 'Bring together volume, origin, quality, and your preferred delivery window.'
        },
        profileCard: {
          title: 'Strengthen your seller profile',
          description: 'Keep the contact details and commercial context buyers need up to date.'
        },
        conversationsCard: {
          title: 'Stay ready for conversations',
          description: 'Future buyer requests and trade conversations will appear here.'
        },
        nextTitle: 'Your seller workspace is ready',
        nextDescription: 'Complete your account details now so your cocoa listings can be introduced with confidence.',
        accountAction: 'Review my seller account'
      },
      user: {
        eyebrow: 'User workspace',
        title: 'Source cocoa with more context and less uncertainty.',
        description: 'Your personal workspace will bring qualified cocoa opportunities and trade conversations into one focused view.',
        discoverCard: {
          title: 'Discover qualified lots',
          description: 'Explore market-ready cocoa opportunities when the marketplace catalogue is available.'
        },
        preferencesCard: {
          title: 'Clarify your sourcing needs',
          description: 'Your account will help you keep origin, volume, and timing preferences visible.'
        },
        conversationsCard: {
          title: 'Build meaningful connections',
          description: 'Future seller conversations and requests will be organised here.'
        },
        nextTitle: 'Your sourcing workspace is ready',
        nextDescription: 'Review your account details so the right cocoa opportunities can find you.',
        accountAction: 'Review my account'
      },
      account: {
        eyebrow: 'Account settings',
        title: 'Your profile and connected browsers.',
        description: 'Review the profile used in this workspace and manage every active browser session for this account.',
        profileTitle: 'Profile',
        name: 'Name',
        email: 'Email address',
        login: 'Login',
        role: 'User type',
        sessionsTitle: 'Connected browsers',
        sessionsDescription: 'Each successful browser login has its own protected session. You can disconnect a browser you no longer use.',
        current: 'This browser',
        remembered: 'Extended session',
        standard: 'Standard session',
        lastActive: 'Last active {{time}}',
        expires: 'Expires {{time}}',
        created: 'Signed in {{time}}',
        disconnect: 'Disconnect',
        disconnectCurrent: 'Sign out this browser',
        noSessions: 'No active browser sessions were found.',
        refreshSessions: 'Refresh sessions',
        loadingSessions: 'Loading your connected browsers…'
      }
    }
  },
  fr: {
    meta: {
      landingTitle: 'CacaoMarketCM | Le commerce du cacao en toute confiance',
      loginTitle: 'CacaoMarketCM | Connexion',
      registrationTitle: 'CacaoMarketCM | Créer votre compte',
      confirmationTitle: 'CacaoMarketCM | Confirmer votre compte',
      administratorDashboardTitle: 'CacaoMarketCM | Espace administrateur',
      sellerDashboardTitle: 'CacaoMarketCM | Espace vendeur',
      userDashboardTitle: 'CacaoMarketCM | Espace utilisateur',
      accountTitle: 'CacaoMarketCM | Paramètres du compte',
      passwordResetRequestTitle: 'CacaoMarketCM | Réinitialiser votre mot de passe',
      passwordResetConfirmationTitle: 'CacaoMarketCM | Choisir un nouveau mot de passe'
    },
    common: {
      brandName: 'CacaoMarketCM',
      homeAria: 'Accueil CacaoMarketCM',
      primaryNavigation: 'Navigation principale',
      languageSelector: 'Choisir la langue',
      switchToLanguage: 'Passer en {{language}}',
      languageNames: {
        english: 'anglais',
        french: 'français'
      },
      languageCodes: {
        english: 'EN',
        french: 'FR'
      }
    },
    notifications: {
      outletAria: 'Notifications',
      dismiss: 'Fermer la notification',
      types: {
        loading: 'En cours',
        success: 'Succès',
        error: 'Erreur',
        warning: 'Attention',
        info: 'Information'
      },
      forms: {
        invalid: 'Vérifiez les champs mis en évidence avant de continuer.'
      },
      api: {
        requestFailed: 'Nous n’avons pas pu terminer la demande. Réessayez.'
      },
      admin: {
        loadFailed: 'Nous ne pouvons pas charger les enregistrements protégés pour le moment.',
        connectionFailed: 'Le navigateur ne peut pas joindre l’API configurée. Vérifiez que le backend est démarré, que le contexte /cacaomarketcm est utilisé, puis vérifiez config.json et, pour une origine différente, CORS.',
        sessionExpired: 'Votre session administrateur n’est plus active. Connectez-vous de nouveau puis réessayez.',
        accessDenied: 'Votre compte actif n’est pas autorisé à lire les enregistrements des tables administrateur.',
        backendDataFailed: 'L’API a été jointe mais ne peut pas terminer l’opération protégée sur la table. Vérifiez la connexion à la base et l’application du schéma gu actuel.',
        backendDataFailedWithRequestId: 'L’API a été jointe mais ne peut pas terminer l’opération protégée sur la table. Vérifiez la base et consultez le journal backend avec l’identifiant de requête {{requestId}}.',
        lookupFailed: 'Nous ne pouvons pas charger une des listes de référence sûres. Actualisez puis réessayez.',
        saving: 'Enregistrement de la modification protégée…',
        saved: 'La modification protégée a été effectuée.',
        saveFailed: 'Nous ne pouvons pas enregistrer cette modification protégée. Vérifiez les valeurs puis réessayez.',
        removing: 'Exécution de l’action protégée…',
        removed: 'L’action protégée a été effectuée.',
        removeFailed: 'Nous ne pouvons pas effectuer cette action protégée. Actualisez puis réessayez.'
      },
      registration: {
        starting: 'Création de votre compte…',
        success: 'Votre demande d’inscription a été acceptée. Consultez votre e-mail pour la confirmer.'
      },
      confirmation: {
        starting: 'Confirmation de votre compte CacaoMarketCM…',
        success: 'Votre compte est confirmé. Redirection vers la connexion…',
        alreadyConfirmed: 'Ce compte était déjà confirmé. Redirection vers la connexion…',
        missingToken: 'Ce lien de confirmation est incomplet. Utilisez le lien complet reçu par e-mail.'
      },
      login: {
        starting: 'Vérification de vos identifiants…',
        success: 'Vous êtes connecté(e).'
      },
      passwordReset: {
        requesting: 'Préparation de votre lien sécurisé de réinitialisation…',
        requestAccepted: 'Si votre compte confirmé utilise cette adresse e-mail, un lien de réinitialisation est en cours d’envoi.',
        completing: 'Mise à jour sécurisée de votre mot de passe…',
        completed: 'Votre mot de passe a été réinitialisé. Redirection vers la connexion…'
      },
      session: {
        signingOut: 'Déconnexion sécurisée en cours…',
        signedOut: 'Vous êtes déconnecté(e).',
        signOutFailed: 'Nous ne pouvons pas vous déconnecter. Réessayez.',
        disconnecting: 'Déconnexion de la session navigateur sélectionnée…',
        disconnected: 'La session navigateur a été déconnectée.',
        disconnectFailed: 'Nous ne pouvons pas déconnecter cette session navigateur. Réessayez.',
        sessionsLoadFailed: 'Nous ne pouvons pas charger vos sessions navigateur pour le moment.'
      },
      language: {
        changed: 'La langue a été changée pour {{language}}.'
      }
    },
    landing: {
      brandTagline: 'Le commerce avec intention',
      navigation: {
        howItWorks: 'Comment ça marche',
        forFarmers: 'Pour les producteurs',
        forBuyers: 'Pour les acheteurs',
        whyCacaoMarketCM: 'Pourquoi CacaoMarketCM'
      },
      actions: {
        joinMarket: 'Rejoindre le marché',
        listBatch: 'Publier un lot de cacao',
        sourceCocoa: 'S’approvisionner avec clarté',
        seeWhereYouFit: 'Voir comment participer',
        growCocoa: 'Je produis du cacao',
        sourceCocoaShort: 'Je recherche du cacao',
        signIn: 'Se connecter'
      },
      hero: {
        badge: 'Pensé pour le commerce sérieux du cacao',
        titleStart: 'Là où les récoltes de cacao rencontrent',
        titleAccent: 'une demande sérieuse.',
        description: 'CacaoMarketCM aide les producteurs à présenter de grands lots de cacao prêts pour le marché et les acheteurs engagés à trouver les volumes, l’origine et le calendrier dont ils ont besoin.',
        largeBatchFocus: 'Des lots importants',
        betterBriefings: 'Des offres plus claires',
        directDialogue: 'Un dialogue direct',
        imageAlt: 'Des producteurs de cacao préparent de grands sacs de cacao dans un point de collecte',
        marketReadyLot: 'Lot prêt pour le marché',
        cocoaInVolume: 'Du cacao en volume',
        clearerStartingPoint: 'Un point de départ plus clair',
        fromHarvestToMarket: 'De la récolte au marché',
        harvestMarketDescription: 'Rendez visibles la disponibilité et l’intention commerciale.',
        tradeBeginsWith: 'Le commerce commence par',
        volumeAndVisibility: 'le volume et la visibilité'
      },
      highlights: {
        lotBasedTitle: 'Des offres par lot',
        lotBasedDescription: 'Construites autour de volumes commerciaux.',
        discoveryTitle: 'Une recherche ciblée',
        discoveryDescription: 'Pour des producteurs et acheteurs engagés.',
        firstStepTitle: 'Un premier pas plus simple',
        firstStepDescription: 'Partagez les bonnes informations dès le départ.'
      },
      value: {
        eyebrow: 'Une place de marché utile',
        title: 'Moins de bruit. De meilleures conversations autour du cacao.',
        description: 'CacaoMarketCM repose sur les informations qui rendent une discussion autour d’un lot de cacao réellement pertinente — avant que l’une ou l’autre partie n’y consacre du temps.',
        designedForTrade: 'Pensé pour le commerce',
        advantages: {
          volume: {
            title: 'Conçu pour le volume',
            description: 'Présentez les récoltes sous forme de lots afin que chaque échange démarre à la bonne échelle.'
          },
          signals: {
            title: 'Des signaux de marché clairs',
            description: 'Partagez dès le départ les éléments recherchés par les acheteurs : origine, disponibilité, quantité et préparation.'
          },
          matches: {
            title: 'Des mises en relation plus ciblées',
            description: 'Offrez aux producteurs et aux acheteurs un espace concentré pour démarrer des échanges commerciaux utiles.'
          }
        }
      },
      process: {
        eyebrow: 'Comment ça marche',
        title: 'Un meilleur chemin du lot de cacao à la discussion commerciale.',
        description: 'CacaoMarketCM donne à chaque partie une manière structurée de présenter ce qu’elle a et ce qu’elle recherche.',
        steps: {
          publish: {
            title: 'Publiez votre lot de cacao',
            description: 'Décrivez le volume disponible, la qualité, la localisation et la période de livraison souhaitée dans une offre claire.'
          },
          demand: {
            title: 'Rencontrez une demande sérieuse',
            description: 'Atteignez des acheteurs qui recherchent des quantités commerciales, pas des échantillons ponctuels ou des échanges imprécis.'
          },
          clarity: {
            title: 'Avancez avec clarté',
            description: 'Comparez les intérêts, discutez des conditions et transformez une mise en relation qualifiée en prochaine étape concrète.'
          }
        }
      },
      farmers: {
        eyebrow: 'Pour les producteurs & coopératives',
        title: 'Rendez votre récolte plus simple à comprendre — et plus difficile à ignorer.',
        description: 'Créez une offre claire autour du volume, de l’origine, de la qualité et de la disponibilité. Laissez les bons acheteurs voir l’opportunité de votre lot.',
        action: 'J’ai du cacao à vendre'
      },
      buyers: {
        eyebrow: 'Pour les acheteurs & équipes sourcing',
        title: 'Commencez avec des lots à la hauteur de votre ambition.',
        description: 'Trouvez des producteurs prêts à discuter de quantités commerciales, puis ouvrez un échange direct avec le contexte nécessaire à votre approvisionnement.',
        action: 'Je souhaite acheter'
      },
      finalCta: {
        eyebrow: 'Un marché du cacao plus connecté',
        title: 'Donnez de la clarté à votre prochaine discussion cacao.',
        description: 'Que vous prépariez un lot important ou cherchiez à vous approvisionner, CacaoMarketCM vous offre un point de départ plus intentionnel.'
      },
      footer: {
        description: 'Aider les producteurs et les acheteurs de cacao à trouver un chemin plus clair vers le commerce.',
        farmers: 'Producteurs',
        buyers: 'Acheteurs',
        about: 'À propos'
      }
    },
    auth: {
      shared: {
        secureAccess: 'Accès sécurisé au marché du cacao',
        backToMarket: 'Retour à CacaoMarketCM',
        footer: 'CacaoMarketCM — le commerce du cacao en toute confiance',
        profileProtection: 'Vos informations sont protégées et utilisées uniquement pour créer votre profil de marché.',
        showPassword: 'Afficher le mot de passe',
        hidePassword: 'Masquer le mot de passe'
      },
      login: {
        eyebrow: 'Bienvenue',
        title: 'Connectez-vous pour faire avancer votre commerce du cacao.',
        description: 'Accédez à votre espace CacaoMarketCM pour gérer votre profil, votre activité de marché et vos conversations.',
        identityLabel: 'Adresse e-mail ou identifiant',
        identityPlaceholder: 'vous@exemple.com ou votre identifiant',
        passwordLabel: 'Mot de passe',
        passwordPlaceholder: 'Saisissez votre mot de passe',
        rememberMe: 'Garder cette session active plus longtemps',
        forgotPassword: 'Mot de passe oublié ?',
        submit: 'Se connecter en toute sécurité',
        submitting: 'Connexion en cours…',
        signedIn: 'Vous êtes connecté(e) en tant que {{name}}.',
        errors: {
          invalidCredentials: 'Votre e-mail, identifiant ou mot de passe est incorrect.',
          pendingConfirmation: 'Confirmez l’e-mail reçu lors de votre inscription avant de vous connecter.',
          requestFailed: 'Nous ne pouvons pas vous connecter pour le moment. Réessayez.'
        },
        noAccount: 'Nouveau sur CacaoMarketCM ?',
        createAccount: 'Créer un compte',
        identityRequired: 'Saisissez votre adresse e-mail ou votre identifiant.',
        passwordRequired: 'Saisissez votre mot de passe.',
        passwordMin: 'Votre mot de passe doit contenir au moins 8 caractères.'
      },
      passwordReset: {
        request: {
          eyebrow: 'Récupération du mot de passe',
          title: 'Réinitialisez votre mot de passe en toute sécurité.',
          description: 'Saisissez l’adresse e-mail de votre compte CacaoMarketCM confirmé. Si elle est éligible, nous vous enverrons un lien de réinitialisation à usage unique.',
          emailLabel: 'Adresse e-mail du compte',
          emailPlaceholder: 'vous@exemple.com',
          emailRequired: 'Saisissez votre adresse e-mail.',
          emailInvalid: 'Saisissez une adresse e-mail valide.',
          submit: 'Envoyer le lien de réinitialisation',
          submitting: 'Envoi du lien sécurisé…',
          successTitle: 'Consultez votre boîte e-mail.',
          successDescription: 'Pour votre sécurité, un lien est envoyé uniquement si cette adresse e-mail appartient à un compte CacaoMarketCM confirmé. Consultez l’e-mail pour connaître son heure d’expiration.',
          backToSignIn: 'Retour à la connexion',
          errors: {
            deliveryUnavailable: 'Nous ne pouvons pas envoyer l’e-mail de réinitialisation pour le moment. Réessayez plus tard.',
            requestFailed: 'Nous ne pouvons pas démarrer votre demande de réinitialisation. Réessayez.'
          }
        },
        confirmation: {
          eyebrow: 'Choisissez un nouveau mot de passe',
          title: 'Créez un nouveau mot de passe sécurisé.',
          description: 'Ce lien personnel ne peut être utilisé qu’une seule fois. La réinitialisation déconnecte ce compte de tous les navigateurs.',
          missingToken: 'Ce lien de réinitialisation est incomplet. Ouvrez le lien complet reçu par e-mail ou demandez-en un nouveau.',
          passwordLabel: 'Nouveau mot de passe',
          passwordPlaceholder: 'Au moins 8 caractères',
          confirmPasswordLabel: 'Confirmez le nouveau mot de passe',
          confirmPasswordPlaceholder: 'Répétez votre nouveau mot de passe',
          passwordRequired: 'Créez un nouveau mot de passe.',
          passwordMin: 'Votre nouveau mot de passe doit contenir au moins 8 caractères.',
          confirmPasswordRequired: 'Confirmez votre nouveau mot de passe.',
          passwordMismatch: 'La confirmation du mot de passe ne correspond pas.',
          submit: 'Réinitialiser le mot de passe',
          submitting: 'Réinitialisation du mot de passe…',
          loadingTitle: 'Vérification de votre lien sécurisé…',
          successTitle: 'Votre mot de passe a été réinitialisé.',
          successDescription: 'Toutes les sessions navigateur de ce compte ont été déconnectées. Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.',
          redirecting: 'Redirection vers la connexion dans {{seconds}} secondes.',
          signInNow: 'Se connecter maintenant',
          requestNewLink: 'Demander un nouveau lien',
          errorTitle: 'Nous n’avons pas pu réinitialiser votre mot de passe.',
          errors: {
            invalid: 'Ce lien de réinitialisation est invalide. Demandez un nouveau lien et utilisez le dernier e-mail reçu.',
            expired: 'Ce lien de réinitialisation a expiré. Demandez-en un nouveau.',
            used: 'Ce lien de réinitialisation a déjà été utilisé. Demandez-en un nouveau si vous avez encore besoin d’aide.',
            unavailable: 'Ce compte n’est pas disponible pour la réinitialisation du mot de passe.',
            passwordInvalid: 'Ce mot de passe n’est pas pris en charge. Utilisez au maximum 72 caractères.',
            requestFailed: 'Nous ne pouvons pas réinitialiser votre mot de passe pour le moment. Réessayez.'
          }
        }
      },
      confirmation: {
        eyebrow: 'Vérification de l’e-mail',
        title: 'Confirmation de votre compte CacaoMarketCM',
        description: 'Nous vérifions de manière sécurisée votre lien personnel de confirmation.',
        loadingTitle: 'Confirmation de votre adresse e-mail…',
        loadingDescription: 'Gardez cette page ouverte pendant l’activation de votre compte.',
        successTitle: 'Votre compte est confirmé.',
        successDescription: 'Vous pouvez maintenant vous connecter et commencer à utiliser CacaoMarketCM.',
        redirecting: 'Redirection vers la connexion dans {{seconds}} secondes.',
        signInNow: 'Se connecter maintenant',
        errorTitle: 'Nous n’avons pas pu confirmer votre compte.',
        missingToken: 'Ce lien de confirmation est incomplet. Ouvrez le lien complet reçu par e-mail ou inscrivez-vous de nouveau.',
        registerAgain: 'Créer un nouveau compte',
        errors: {
          invalid: 'Ce lien de confirmation est invalide. Demandez un nouveau lien en vous inscrivant à nouveau.',
          expired: 'Ce lien de confirmation a expiré. Veuillez vous inscrire de nouveau.',
          alreadyConfirmed: 'Ce compte a déjà été confirmé. Vous pouvez vous connecter.',
          unavailable: 'Ce lien de confirmation ne peut pas activer ce compte. Veuillez vous inscrire de nouveau.',
          requestFailed: 'Nous ne pouvons pas confirmer votre compte pour le moment. Réessayez.'
        }
      },
      registration: {
        eyebrow: 'Créez votre profil de marché',
        title: 'Préparez votre prochaine transaction cacao avec clarté.',
        description: 'Créez un compte pour présenter votre intérêt à vendre ou à rechercher des lots de cacao commerciaux.',
        chooseRole: 'Comment allez-vous utiliser CacaoMarketCM ?',
        sellerTitle: 'Je souhaite vendre du cacao',
        sellerDescription: 'Pour les producteurs, coopératives et vendeurs ayant des lots de cacao à présenter.',
        buyerTitle: 'Je souhaite rechercher du cacao',
        buyerDescription: 'Pour les acheteurs et équipes sourcing qui recherchent des quantités commerciales.',
        clientProfileTitle: 'Sous quel statut recherchez-vous du cacao ?',
        clientProfileDescription: 'Choisissez le profil juridique qui portera ce compte acheteur.',
        individualTitle: 'Personne physique',
        individualDescription: 'Je recherche du cacao en mon nom propre.',
        enterpriseTitle: 'Personne morale / entreprise',
        enterpriseDescription: 'Je recherche du cacao pour le compte d’une entreprise immatriculée.',
        clientProfileRequired: 'Choisissez si ce compte acheteur est créé pour une personne physique ou une entreprise.',
        enterpriseDetailsTitle: 'Identification de l’entreprise',
        enterpriseDetailsDescription: 'Saisissez les informations légales de l’entreprise liées à ce compte acheteur.',
        companyNameLabel: 'Raison sociale',
        companyNamePlaceholder: 'Dénomination légale de l’entreprise',
        companyNameRequired: 'Saisissez la raison sociale de l’entreprise.',
        niuLabel: 'NIU',
        niuPlaceholder: 'Numéro d’identifiant unique',
        niuRequired: 'Saisissez le NIU de l’entreprise.',
        rccmLabel: 'RCCM',
        rccmPlaceholder: 'Numéro du Registre du Commerce et du Crédit Mobilier',
        rccmRequired: 'Saisissez le RCCM de l’entreprise.',
        representativeNotice: 'Les prénom et nom ci-dessous identifient le représentant légal ou le contact principal de l’entreprise.',
        representativeFirstNameLabel: 'Prénom du représentant légal',
        representativeFirstNamePlaceholder: 'Prénom du représentant',
        representativeFirstNameRequired: 'Saisissez le prénom du représentant légal.',
        representativeLastNameLabel: 'Nom du représentant légal',
        representativeLastNamePlaceholder: 'Nom du représentant',
        representativeLastNameRequired: 'Saisissez le nom du représentant légal.',
        firstNameLabel: 'Prénom',
        firstNamePlaceholder: 'Votre prénom',
        lastNameLabel: 'Nom',
        lastNamePlaceholder: 'Votre nom',
        emailLabel: 'Adresse e-mail',
        emailPlaceholder: 'vous@exemple.com',
        loginLabel: 'Choisissez un identifiant',
        loginPlaceholder: 'Votre identifiant préféré',
        passwordLabel: 'Créez un mot de passe',
        passwordPlaceholder: 'Au moins 8 caractères',
        confirmPasswordLabel: 'Confirmez le mot de passe',
        confirmPasswordPlaceholder: 'Répétez votre mot de passe',
        agreement: 'J’accepte de créer un compte CacaoMarketCM pour des échanges liés au marché.',
        submit: 'Créer mon compte',
        submitting: 'Envoi de l’e-mail de confirmation…',
        confirmationSent: 'Un e-mail de confirmation a été envoyé à {{email}}.',
        confirmationExpiry: 'Ouvrez son lien personnel dans les 3 heures. Les inscriptions non confirmées expirent et sont supprimées automatiquement.',
        errors: {
          identityExists: 'Un compte utilise déjà cette adresse e-mail ou cet identifiant.',
          enterpriseIdentifierExists: 'Un compte utilise déjà l’un de ces identifiants d’entreprise.',
          identityOrEnterpriseIdentifierExists: 'Un compte utilise déjà cette adresse e-mail, cet identifiant ou un identifiant d’entreprise.',
          clientProfileInvalid: 'Vérifiez le profil acheteur et les informations de l’entreprise, puis réessayez.',
          deliveryUnavailable: 'Nous n’avons pas pu envoyer votre e-mail de confirmation. Réessayez plus tard.',
          requestFailed: 'Nous n’avons pas pu démarrer votre inscription. Vérifiez vos informations et réessayez.'
        },
        alreadyAccount: 'Vous avez déjà un compte ?',
        signIn: 'Se connecter',
        roleRequired: 'Choisissez comment vous utiliserez CacaoMarketCM.',
        firstNameRequired: 'Saisissez votre prénom.',
        lastNameRequired: 'Saisissez votre nom.',
        emailRequired: 'Saisissez votre adresse e-mail.',
        emailInvalid: 'Saisissez une adresse e-mail valide.',
        loginRequired: 'Choisissez un identifiant.',
        loginMin: 'Votre identifiant doit contenir au moins 3 caractères.',
        passwordRequired: 'Créez un mot de passe.',
        passwordMin: 'Votre mot de passe doit contenir au moins 8 caractères.',
        confirmPasswordRequired: 'Confirmez votre mot de passe.',
        passwordMismatch: 'La confirmation du mot de passe ne correspond pas.',
        agreementRequired: 'Veuillez confirmer votre accord pour continuer.'
      }
    },
    dashboard: {
      roles: {
        administrator: 'Administrateur',
        seller: 'Vendeur',
        user: 'Utilisateur'
      },
      header: {
        overview: 'Vue d’ensemble',
        accountSettings: 'Paramètres du compte',
        signOut: 'Se déconnecter',
        accountMenu: 'Menu du compte',
        signedInAs: 'Connecté(e) en tant que {{role}}',
        openAccountMenu: 'Ouvrir le menu du compte'
      },
      shared: {
        workspace: 'Espace CacaoMarketCM',
        ready: 'Prêt pour votre prochaine étape',
        nextStep: 'Prochaine étape suggérée',
        viewAccount: 'Voir les paramètres du compte',
        secureWorkspace: 'Votre espace est protégé par une session navigateur.'
      },
      admin: {
        eyebrow: 'Espace administrateur',
        title: 'Faites avancer l’écosystème CacaoMarketCM en toute confiance.',
        description: 'Votre espace opérationnel pour superviser les accès, l’activité commerciale et l’expérience de la plateforme.',
        accessCard: {
          title: 'Gouvernance des accès',
          description: 'Examinez les rôles et assurez-vous que les bonnes personnes sont connectées à la place de marché.'
        },
        sessionsCard: {
          title: 'Navigateurs connectés',
          description: 'Les contrôles de session rendent l’accès aux comptes plus réfléchi et plus sécurisé.'
        },
        marketCard: {
          title: 'Préparation du marché',
          description: 'Préparez la vue opérationnelle pour la prochaine activité autour du cacao.'
        },
        nextTitle: 'Donnez le rythme opérationnel',
        nextDescription: 'Votre tableau de bord administrateur est prêt pour la gestion des utilisateurs, les contrôles de rôles et les futurs modules de supervision du marché.',
        accountAction: 'Ouvrir mes paramètres de compte',
        tableManagement: {
          eyebrow: 'Contrôles du schéma',
          title: 'Gérer les tables de données gu protégées',
          description: 'Ouvrez un écran de gestion dédié pour chaque table du schéma. Les enregistrements de sécurité restent auditables et proposent uniquement des actions sûres et limitées.',
          tablesAvailable: 'tables disponibles',
          manage: 'Gérer la table',
          audit: 'Audit sécurisé'
        },
        tables: {
          userTypes: {
            title: 'Types d’utilisateur',
            description: 'Gérez le catalogue des rôles utilisés par les comptes.',
            securityNote: 'Les rôles intégrés CLIENT, VENDEUR et ADMINISTRATEUR sont protégés contre la suppression et les modifications de code risquées.'
          },
          users: {
            title: 'Utilisateurs',
            description: 'Créez, modifiez, suspendez ou supprimez des comptes applicatifs.',
            securityNote: 'Les mots de passe sont acceptés uniquement lors de la création contrôlée d’un compte et ne sont jamais affichés, modifiés ou renvoyés par cet écran. Les comptes CLIENT sont créés par le parcours d’inscription afin que leur profil juridique soit toujours enregistré.'
          },
          individualClients: {
            title: 'Profils acheteurs personnes physiques',
            description: 'Auditez les comptes CLIENT inscrits comme personnes physiques.',
            securityNote: 'Il s’agit d’une vue d’audit de la relation de profil. Le type personne physique est choisi lors de l’inscription sécurisée et ne peut pas être supprimé indépendamment du compte.'
          },
          enterpriseClients: {
            title: 'Profils acheteurs entreprises',
            description: 'Consultez et corrigez les informations légales des acheteurs entreprises enregistrés.',
            securityNote: 'Seules les données de l’entreprise sont modifiables ici. Le NIU et le RCCM restent uniques et le type de profil ne peut pas être modifié indépendamment du compte CLIENT.'
          },
          sessions: {
            title: 'Sessions navigateur',
            description: 'Auditez les navigateurs connectés et révoquez un accès si nécessaire.',
            securityNote: 'Il s’agit d’une vue d’audit et de révocation. Les empreintes de session et les cookies navigateur ne sont jamais affichés.'
          },
          confirmations: {
            title: 'Confirmations d’inscription',
            description: 'Auditez le cycle de vie des confirmations de compte.',
            securityNote: 'Il s’agit d’une vue d’audit. Les jetons et empreintes de jeton ne sont jamais affichés ; seule une inscription en attente non confirmée peut être annulée.'
          },
          passwordResets: {
            title: 'Demandes de réinitialisation',
            description: 'Auditez les liens de réinitialisation et révoquez les liens inutilisés.',
            securityNote: 'Il s’agit d’une vue d’audit et de révocation. Les jetons de réinitialisation et leurs empreintes ne sont jamais affichés.'
          },
          basicRights: {
            title: 'Droits de base',
            description: 'Gérez les capacités nommées de l’application.',
            securityNote: 'La capacité obligatoire APP-CONN reste protégée. Les droits sont attribués aux types dans la table d’association distincte.'
          },
          rightAssignments: {
            title: 'Attributions type-droit',
            description: 'Attribuez un droit de base à un type d’utilisateur ou supprimez une attribution autorisée.',
            securityNote: 'Les attributions de l’administrateur sont protégées par la base de données et ne peuvent pas être supprimées depuis cet écran.'
          },
          passwordHistory: {
            title: 'Historique des mots de passe',
            description: 'Auditez les métadonnées du cycle de vie des mots de passe sans donnée de sécurité.',
            securityNote: 'Cette table est strictement en lecture seule. Les valeurs et empreintes de mots de passe ne sont jamais affichées ni modifiables.'
          }
        },
        management: {
          eyebrow: 'Gestion des tables administrateur',
          auditOnly: 'Vue audit sécurisée',
          back: 'Retour au tableau de bord administrateur',
          createEyebrow: 'Nouvel enregistrement contrôlé',
          editEyebrow: 'Mise à jour contrôlée',
          createTitle: 'Créer un enregistrement',
          editTitle: 'Mettre à jour un enregistrement',
          temporaryPasswordNotice: 'Le mot de passe temporaire est envoyé uniquement au serveur sécurisé pour être haché une seule fois. Il ne sera jamais affiché de nouveau.',
          selectOption: 'Sélectionnez une option',
          invalidField: 'Saisissez une valeur valide pour ce champ.',
          confirmActionTitle: 'Confirmer l’action protégée',
          confirmActionDescription: 'Confirmez cette action pour l’enregistrement n°{{id}}. Cette action est journalisée et peut révoquer immédiatement un accès.',
          recordsTitle: 'Vue sûre des enregistrements',
          recordsDescription: 'Seuls les champs de métadonnées approuvés sont visibles ici.',
          recordsCount: 'enregistrements',
          loading: 'Chargement des enregistrements protégés…',
          loadError: 'Les enregistrements protégés ne peuvent pas être chargés pour le moment.',
          noRecords: 'Aucun enregistrement approuvé n’est disponible pour cette table.'
        },
        actions: {
          add: 'Ajouter un enregistrement',
          refresh: 'Actualiser',
          edit: 'Modifier',
          delete: 'Supprimer',
          revoke: 'Révoquer la session',
          cancelPending: 'Annuler l’inscription',
          revokeReset: 'Révoquer le lien',
          removeAssignment: 'Supprimer l’attribution',
          cancel: 'Annuler',
          createRecord: 'Créer l’enregistrement',
          saveChanges: 'Enregistrer les modifications',
          tryAgain: 'Réessayer'
        },
        columns: {
          id: 'ID',
          code: 'Code',
          name: 'Nom',
          firstName: 'Prénom',
          lastName: 'Nom',
          email: 'E-mail',
          login: 'Identifiant',
          userType: 'Type d’utilisateur',
          status: 'Statut',
          createdAt: 'Créé le',
          browser: 'Navigateur',
          extended: 'Prolongée',
          lastSeenAt: 'Dernière activité',
          expiresAt: 'Expire le',
          revokedAt: 'Révoquée le',
          confirmedAt: 'Confirmée le',
          usedAt: 'Utilisé le',
          basicRight: 'Droit de base',
          currentPassword: 'Actuel',
          companyName: 'Raison sociale',
          niu: 'NIU',
          rccm: 'RCCM',
          representativeFirstName: 'Prénom du représentant',
          representativeLastName: 'Nom du représentant',
          recordedAt: 'Enregistré le',
          changedAt: 'Modifié le',
          actions: 'Actions'
        },
        fields: {
          code: 'Code',
          name: 'Nom',
          userType: 'Type d’utilisateur',
          firstName: 'Prénom',
          lastName: 'Nom',
          email: 'Adresse e-mail',
          login: 'Identifiant',
          status: 'Statut',
          temporaryPassword: 'Mot de passe temporaire',
          basicRight: 'Droit de base',
          companyName: 'Raison sociale',
          niu: 'NIU',
          rccm: 'RCCM'
        },
        statuses: {
          active: 'Actif',
          suspended: 'Suspendu',
          pending: 'En attente de confirmation'
        },
        values: {
          yes: 'Oui',
          no: 'Non'
        }
      },
      seller: {
        eyebrow: 'Espace vendeur',
        title: 'Transformez votre disponibilité cacao en une offre de marché claire.',
        description: 'Voici votre espace pour préparer vos lots, présenter leur disponibilité et suivre l’intérêt des acheteurs.',
        prepareCard: {
          title: 'Préparez votre prochain lot',
          description: 'Réunissez le volume, l’origine, la qualité et votre période de livraison préférée.'
        },
        profileCard: {
          title: 'Renforcez votre profil vendeur',
          description: 'Gardez à jour les coordonnées et le contexte commercial dont les acheteurs ont besoin.'
        },
        conversationsCard: {
          title: 'Restez prêt pour les échanges',
          description: 'Les futures demandes acheteurs et conversations commerciales apparaîtront ici.'
        },
        nextTitle: 'Votre espace vendeur est prêt',
        nextDescription: 'Complétez vos informations de compte dès maintenant afin que vos offres de cacao soient présentées avec confiance.',
        accountAction: 'Vérifier mon compte vendeur'
      },
      user: {
        eyebrow: 'Espace utilisateur',
        title: 'Approvisionnez-vous en cacao avec plus de contexte et moins d’incertitude.',
        description: 'Votre espace personnel réunira les opportunités cacao qualifiées et les conversations commerciales dans une vue ciblée.',
        discoverCard: {
          title: 'Découvrez des lots qualifiés',
          description: 'Explorez les opportunités cacao prêtes pour le marché lorsque le catalogue sera disponible.'
        },
        preferencesCard: {
          title: 'Précisez vos besoins d’approvisionnement',
          description: 'Votre compte vous aidera à garder visibles vos préférences d’origine, de volume et de calendrier.'
        },
        conversationsCard: {
          title: 'Créez des relations utiles',
          description: 'Les futures conversations et demandes aux vendeurs seront organisées ici.'
        },
        nextTitle: 'Votre espace approvisionnement est prêt',
        nextDescription: 'Vérifiez vos informations de compte afin que les bonnes opportunités cacao puissent vous trouver.',
        accountAction: 'Vérifier mon compte'
      },
      account: {
        eyebrow: 'Paramètres du compte',
        title: 'Votre profil et vos navigateurs connectés.',
        description: 'Vérifiez le profil utilisé dans cet espace et gérez chaque session navigateur active de ce compte.',
        profileTitle: 'Profil',
        name: 'Nom',
        email: 'Adresse e-mail',
        login: 'Identifiant',
        role: 'Type d’utilisateur',
        sessionsTitle: 'Navigateurs connectés',
        sessionsDescription: 'Chaque connexion navigateur réussie possède sa propre session protégée. Vous pouvez déconnecter un navigateur que vous n’utilisez plus.',
        current: 'Ce navigateur',
        remembered: 'Session prolongée',
        standard: 'Session standard',
        lastActive: 'Dernière activité {{time}}',
        expires: 'Expire {{time}}',
        created: 'Connexion {{time}}',
        disconnect: 'Déconnecter',
        disconnectCurrent: 'Se déconnecter de ce navigateur',
        noSessions: 'Aucune session navigateur active n’a été trouvée.',
        refreshSessions: 'Actualiser les sessions',
        loadingSessions: 'Chargement de vos navigateurs connectés…'
      }
    }
  }
} as const satisfies Record<'en' | 'fr', TranslationDictionary>;
