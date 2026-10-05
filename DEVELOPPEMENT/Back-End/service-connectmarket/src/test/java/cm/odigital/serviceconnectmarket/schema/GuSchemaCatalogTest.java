package cm.odigital.serviceconnectmarket.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import cm.odigital.serviceconnectmarket.auth.admin.domain.AdminTable;

class GuSchemaCatalogTest {

    @Test
    void coversExactlyTheTablesExposedByTheAdministratorApi() {
        List<String> catalogued = relationNames();

        assertEquals(AdminTable.values().length, catalogued.size());
        for (AdminTable table : AdminTable.values()) {
            assertTrue(
                catalogued.contains(table.apiKey()),
                () -> "Missing startup check for " + table.apiKey()
            );
        }
    }

    @Test
    void listsEveryScriptRelationOnce() {
        List<String> names = relationNames();

        assertEquals(names.size(), names.stream().distinct().count());
        assertTrue(names.contains("sessions_utilisateur"));
        assertTrue(names.contains("password_history"));
    }

    @Test
    void buildsReadOnlyProbesForRelationsAndColumns() {
        GuSchemaCatalog.GuRelation typeUtilisateur = GuSchemaCatalog.GU_RELATIONS.get(0);

        assertEquals("gu.type_utilisateur", typeUtilisateur.qualifiedName());
        assertEquals("SELECT 1 FROM gu.type_utilisateur LIMIT 0", typeUtilisateur.relationProbe());
        assertEquals("SELECT code FROM gu.type_utilisateur LIMIT 0", typeUtilisateur.columnProbe("code"));
    }

    private List<String> relationNames() {
        return GuSchemaCatalog.GU_RELATIONS.stream().map(GuSchemaCatalog.GuRelation::name).toList();
    }
}
