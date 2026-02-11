package at.jku.se.gruppe2.infrastructure.persistence;

import at.jku.se.gruppe2.domain.model.automation.Rule;
import at.jku.se.gruppe2.infrastructure.persistence.repository.RuleRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RuleRepoTest extends DbTestBase{

    @Test
    void createRule_persistsAndReturnsGeneratedId() {
        int addrId = insertAddress("A", "1", "4020", "Linz", "AT", null, null);
        int homeId = insertHome(1, "H", addrId);

        RuleRepository repo = new RuleRepository();

        Rule rule = new Rule();
        rule.setHomeId(homeId);
        rule.setName("My Rule");
        rule.setEnabled(true);
        rule.setPriority(5);
        rule.setConditionJson("{\"if\":\"temp>20\"}");
        rule.setActionJson("{\"then\":\"fan:on\"}");

        int id = repo.createRule(rule);

        assertTrue(id > 0);
        assertEquals(id, rule.getId());

        Rule loaded = repo.findById(id).orElseThrow();
        assertEquals("My Rule", loaded.getName());
        assertTrue(loaded.isEnabled());
        assertEquals(5, loaded.getPriority());
        assertEquals("{\"if\":\"temp>20\"}", loaded.getConditionJson());
        assertEquals("{\"then\":\"fan:on\"}", loaded.getActionJson());

        // created_at / updated_at are set by DB; repository maps to Instant (may be null if DB returns null, but should not)
        assertNotNull(loaded.getCreatedAt());
        assertNotNull(loaded.getUpdatedAt());
    }

    @Test
    void updateRule_updatesFieldsAndAdvancesUpdatedAt() throws InterruptedException {
        int addrId = insertAddress("A", "1", "4020", "Linz", "AT", null, null);
        int homeId = insertHome(1, "H", addrId);

        RuleRepository repo = new RuleRepository();

        Rule rule = new Rule();
        rule.setHomeId(homeId);
        rule.setName("Initial");
        rule.setEnabled(true);
        rule.setPriority(1);
        rule.setConditionJson("{}");
        rule.setActionJson("{}");

        int id = repo.createRule(rule);
        Rule before = repo.findById(id).orElseThrow();
        Instant beforeUpdated = before.getUpdatedAt();

        // Ensure updated_at tick differs (some DBs only have second precision)
        Thread.sleep(1100);

        rule.setName("Updated");
        rule.setEnabled(false);
        rule.setPriority(10);
        rule.setConditionJson("{\"c\":1}");
        rule.setActionJson("{\"a\":2}");

        int affected = repo.updateRule(rule);
        assertEquals(1, affected);

        Rule after = repo.findById(id).orElseThrow();
        assertEquals("Updated", after.getName());
        assertFalse(after.isEnabled());
        assertEquals(10, after.getPriority());
        assertEquals("{\"c\":1}", after.getConditionJson());
        assertEquals("{\"a\":2}", after.getActionJson());

        assertNotNull(after.getUpdatedAt());
        assertTrue(after.getUpdatedAt().isAfter(beforeUpdated),
                "Expected updated_at to be advanced after update");
    }

    @Test
    void setEnabled_togglesEnabledAndFindAllEnabledFilters() {
        int addrId = insertAddress("A", "1", "4020", "Linz", "AT", null, null);
        int homeId = insertHome(1, "H", addrId);

        RuleRepository repo = new RuleRepository();

        int r1 = repo.createRule(rule(homeId, "R1", true, 1));
        int r2 = repo.createRule(rule(homeId, "R2", true, 2));

        List<Rule> enabled = repo.findAllEnabledByHomeId(homeId);
        assertEquals(2, enabled.size());

        assertEquals(1, repo.setEnabled(r2, false));

        enabled = repo.findAllEnabledByHomeId(homeId);
        assertEquals(1, enabled.size());
        assertEquals("R1", enabled.get(0).getName());

        Rule loadedR2 = repo.findById(r2).orElseThrow();
        assertFalse(loadedR2.isEnabled());
    }

    @Test
    void findAllByHomeId_ordersByPriorityDescThenUpdatedAtDesc() throws InterruptedException {
        int addrId = insertAddress("A", "1", "4020", "Linz", "AT", null, null);
        int homeId = insertHome(1, "H", addrId);

        RuleRepository repo = new RuleRepository();

        // Same priority; order should be updated_at DESC
        Rule a = rule(homeId, "SamePrio-A", true, 5);
        int idA = repo.createRule(a);

        Thread.sleep(1100);

        Rule b = rule(homeId, "SamePrio-B", true, 5);
        int idB = repo.createRule(b);

        // Different priority; should come first regardless of time
        Rule high = rule(homeId, "HighPrio", true, 99);
        int idHigh = repo.createRule(high);

        List<Rule> all = repo.findAllByHomeId(homeId);
        assertEquals(3, all.size());

        assertEquals(idHigh, all.get(0).getId());

        // For same priority 5: B created later than A => B should come before A
        assertEquals(idB, all.get(1).getId());
        assertEquals(idA, all.get(2).getId());
    }

    @Test
    void deleteRule_removesRule() {
        int addrId = insertAddress("A", "1", "4020", "Linz", "AT", null, null);
        int homeId = insertHome(1, "H", addrId);

        RuleRepository repo = new RuleRepository();

        int id = repo.createRule(rule(homeId, "ToDelete", true, 0));
        assertTrue(repo.findById(id).isPresent());

        assertEquals(1, repo.deleteRule(id));
        assertTrue(repo.findById(id).isEmpty());
    }

    private Rule rule(int homeId, String name, boolean enabled, int priority) {
        Rule r = new Rule();
        r.setHomeId(homeId);
        r.setName(name);
        r.setEnabled(enabled);
        r.setPriority(priority);
        r.setConditionJson("{\"cond\":true}");
        r.setActionJson("{\"act\":true}");
        return r;
    }

}