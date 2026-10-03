import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;

/**
 * Console entry point: loads the saved catalog, lets the user pick a role, and delegates
 * that role's menu to its {@link RoleCliHandler}.
 */
public final class CampusCli {
    private final CliContext context;
    private final List<RoleCliHandler> handlers;

    public CampusCli() {
        this(new BufferedReader(new InputStreamReader(System.in)), System.out);
    }

    CampusCli(BufferedReader input, PrintStream output) {
        this(input, output, new CampusPersistence());
    }

    CampusCli(BufferedReader input, PrintStream output, CampusPersistence persistence) {
        this.context = new CliContext(input, output, persistence);
        this.handlers = List.of(
                new AdminCliHandler(context),
                InstructorCliHandler.permanent(context),
                InstructorCliHandler.visiting(context),
                new TeachingAssistantCliHandler(context),
                new StudentCliHandler(context));
    }

    public void run() {
        ApplicationLogger.info("application.started");
        try {
            context.loadCatalog();
        } catch (IOException exception) {
            ApplicationLogger.error("catalog.load_failed", exception);
            context.out().println("Unable to load the saved catalog: " + exception.getMessage());
            return;
        }
        try {
            runRoleSelection();
        } catch (IOException exception) {
            ApplicationLogger.error("application.input_failure", exception);
            context.out().println("Unable to continue reading input. Goodbye.");
        }
    }

    private void runRoleSelection() throws IOException {
        while (true) {
            context.out().println("\nCampus Management System");
            context.out().println("0. Exit");
            for (int index = 0; index < handlers.size(); index++) {
                context.out().printf("%d. %s%n", index + 1, handlers.get(index).roleName());
            }
            Integer selection = context.readChoice("Select a role: ", 0, handlers.size());
            if (selection == null || selection == 0) {
                ApplicationLogger.info("application.stopped");
                context.out().println("Goodbye.");
                return;
            }
            RoleCliHandler handler = handlers.get(selection - 1);
            if (handler.signIn()) {
                runRoleMenu(handler);
            }
        }
    }

    private void runRoleMenu(RoleCliHandler handler) throws IOException {
        List<String> options = handler.menuOptions();
        while (true) {
            context.out().printf("%n%s Menu%n", handler.roleName());
            context.out().println("0. Back to role selection");
            for (int index = 0; index < options.size(); index++) {
                context.out().printf("%d. %s%n", index + 1, options.get(index));
            }
            Integer selection = context.readChoice("Select an option: ", 0, options.size());
            if (selection == null) {
                context.out().println("Goodbye.");
                return;
            }
            if (selection == 0) {
                return;
            }
            handler.handle(selection);
        }
    }
}
