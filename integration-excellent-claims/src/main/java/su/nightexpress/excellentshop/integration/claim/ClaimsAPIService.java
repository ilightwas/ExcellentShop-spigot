package su.nightexpress.excellentshop.integration.claim;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentclaims.api.ClaimsAPI;

@NullMarked
public class ClaimsAPIService {

    private final ClaimsAPI claimsAPI;

    public ClaimsAPIService() {
        RegisteredServiceProvider<ClaimsAPI> provider = Bukkit.getServicesManager().getRegistration(ClaimsAPI.class);
        if (provider == null) {
            throw new IllegalStateException("ExcellentClaims API is not loaded!");
        }
        this.claimsAPI = provider.getProvider();
    }

    public ClaimsAPI getClaimsAPI() {
        return this.claimsAPI;
    }
}