package su.nightexpress.excellentshop.integration.claim;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.excellentclaims.api.claim.Claim;
import su.nightexpress.excellentclaims.api.claim.OwnableClaim;
import su.nightexpress.excellentshop.api.claim.ClaimHook;

@NullMarked
public class ExcellentClaimsHook implements ClaimHook {

    private final ClaimsAPIService apiService;

    public ExcellentClaimsHook() {
        this.apiService = new ClaimsAPIService();
    }

    @Override
    public boolean isInOwnClaim(Player player, Block block) {
        Location location = player.getLocation();
        if (location == null) return false;

        Claim claim = this.apiService.getClaimsAPI().getPrioritizedClaim(location);
        if (claim == null || claim.isBackgroundClaim()) return false;

        return claim instanceof OwnableClaim ownable && ownable.isOwner(player);
    }
}
