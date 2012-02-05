package mgmt;


import javax.security.auth.callback.*;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * This class provides both a CallbackHandler and an Authenticator to handle the client side authentication
 * requirements for the demos.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DemoAuthentication {

    private static final DemoAuthentication INSTANCE = new DemoAuthentication();

    private final CallbackHandler CALLBACK_HANDLER = new DemoCallbackHandler();
    private final Authenticator AUTHENTICATOR = new DemoAuthenticator();

    // After the demo has connected the physical connection may be re-established numerous times.
    // for this reason we cache the entered values to allow for re-use without pestering the end
    // user.
    private boolean promptShown = false;
    private String userName = null;
    private char[] password = null;

    private DemoAuthentication() {
    }

    public static CallbackHandler getCallbackHandler() {
        return INSTANCE.CALLBACK_HANDLER;
    }

    public static Authenticator getAuthenticator() {
        return INSTANCE.AUTHENTICATOR;
    }

    void prompt(final String realm) {
        if (promptShown == false) {
            promptShown = true;
            System.out.println("Authenticating against security realm: " + realm);
        }
    }

    String obtainUsername(final String prompt) {
        if (userName == null) {
            userName = System.console().readLine(prompt);
        }
        return userName;
    }

    char[] obtainPassword(final String prompt) {
        if (password == null) {
            password = System.console().readPassword(prompt);
        }

        return password;
    }

    class DemoCallbackHandler implements CallbackHandler {

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

            // Special case for anonymous authentication to avoid prompting user for their name.
            if (callbacks.length == 1 && callbacks[0] instanceof NameCallback) {
                ((NameCallback) callbacks[0]).setName("anonymous demo user");
                return;
            }

            for (Callback current : callbacks) {
                if (current instanceof RealmCallback) {
                    RealmCallback rcb = (RealmCallback) current;
                    String defaultText = rcb.getDefaultText();
                    rcb.setText(defaultText); // For now just use the realm suggested.

                    prompt(defaultText);
                } else if (current instanceof RealmChoiceCallback) {
                    throw new UnsupportedCallbackException(current, "Realm choice not currently supported.");
                } else if (current instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) current;
                    String userName = obtainUsername("Username:");

                    ncb.setName(userName);
                } else if (current instanceof PasswordCallback) {
                    PasswordCallback pcb = (PasswordCallback) current;
                    char[] password = obtainPassword("Password:");

                    pcb.setPassword(password);
                } else {
                    throw new UnsupportedCallbackException(current);
                }

            }
        }

    }

    class DemoAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            prompt(getRequestingPrompt());
            String userName = obtainUsername("Username:");
            char[] password = obtainPassword("Password:");

            return new PasswordAuthentication(userName, password);
        }
    }

}
