package test;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
class UserBattlePassEntityTest {
    @Builder.Default
    private Boolean isPremium = false;
    @Builder.Default
    private Integer claimedFreeLevel = 0;
    @Builder.Default
    private Integer claimedPremiumLevel = 0;
}
public class Test {
    public static void main(String[] args) {
        UserBattlePassEntityTest u = UserBattlePassEntityTest.builder().build();
        System.out.println(\"isPremium: \" + u.getIsPremium());
        System.out.println(\"claimedFreeLevel: \" + u.getClaimedFreeLevel());
    }
}
