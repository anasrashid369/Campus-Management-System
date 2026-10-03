import java.io.IOException;
import java.util.List;

/** Menu and workflows for one campus role, driven by {@link CampusCli}. */
interface RoleCliHandler {
    String roleName();

    /** Menu labels; option {@code n} in the menu is element {@code n - 1}. */
    List<String> menuOptions();

    /** Selects the acting user for this role. Returns false to go back to role selection. */
    default boolean signIn() throws IOException {
        return true;
    }

    /** Runs the workflow for a 1-based menu option. */
    void handle(int option) throws IOException;
}
