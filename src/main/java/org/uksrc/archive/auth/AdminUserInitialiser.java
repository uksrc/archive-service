package org.uksrc.archive.auth;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * AdminUserInitialiser is a class responsible for initialising the admin user
 * during the startup event of the application. If an admin user with the specified
 * username does not exist in the database, it creates a new admin user with the provided
 * credentials and assigns the "admin" role to that user.
 */
@ApplicationScoped
public class AdminUserInitialiser {

    static final String ADMIN_ROLE = "admin";   /** Roles applied to the admin user (CSV) */
    static final String USERNAME_KEY = "login"; /** Key for the username column */

    @ConfigProperty(name = "basic.auth.admin.username")
    String username;    // Username of the admin user, via application.properties

    @ConfigProperty(name = "basic.auth.admin.password")
    String password;    // Password of the admin user, via application.properties

    /**
     * Handles the startup event of the application. This method ensures that an admin user
     * with the specified username exists in the database. If no such user is found, it creates
     * a new admin user with the credentials defined in the configuration and assigns the "admin"
     * role to that user.
     *
     * @param ev the startup event that triggers this method. Provides context for application startup.
     */
    void onStart(@Observes StartupEvent ev) {
        if (AdminUser.count(USERNAME_KEY, username) == 0) {
            AdminUser.add(username, password, ADMIN_ROLE);
            System.out.println("✅ Admin user created: " + username);
        } else {
            System.out.println("✅ Admin user already exists.");
        }
    }
}
