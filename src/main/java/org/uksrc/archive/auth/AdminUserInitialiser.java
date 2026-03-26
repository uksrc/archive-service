package org.uksrc.archive.auth;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AdminUserInitialiser {

    @ConfigProperty(name = "basic.auth.admin.username")
    String username;

    @ConfigProperty(name = "basic.auth.admin.password")
    String password;

    void onStart(@Observes StartupEvent ev) {

        if (AdminUser.count("login", username) == 0) {
            AdminUser.add(username, password, "admin");
        }
    }
}
