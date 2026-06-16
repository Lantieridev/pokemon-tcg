package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.pipeline.abilities.*;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves a {@link AbilityEffectId} into an executable {@link AbilityEffect}
 * using pre-registered strategy handlers.
 */
public final class AbilityEffectResolver {

    private final Map<AbilityEffectId, AbilityEffect> strategies;

    /**
     * Constructs the resolver and registers the strategy handlers for each ability.
     */
    public AbilityEffectResolver() {
        final Map<AbilityEffectId, AbilityEffect> map = new EnumMap<>(AbilityEffectId.class);
        
        map.put(AbilityEffectId.FAIRY_TRANSFER, new FairyTransferStrategy(PokemonType.FAIRY, List.of("rainbow")));
        map.put(AbilityEffectId.MYSTICAL_FIRE, new DrawUntilHandSizeStrategy(6));
        map.put(AbilityEffectId.MAGNETIC_DRAW, new DrawUntilHandSizeStrategy(4));
        map.put(AbilityEffectId.DRIVE_OFF, new DriveOffStrategy());
        map.put(AbilityEffectId.WATER_SHURIKEN, new WaterShurikenStrategy(PokemonType.WATER, 3));
        map.put(AbilityEffectId.STANCE_CHANGE, new StanceChangeStrategy());
        map.put(AbilityEffectId.UPSIDE_DOWN_EVOLUTION, new UpsideDownEvolutionStrategy());
        map.put(AbilityEffectId.LEAF_DRAW, new LeafDrawStrategy());
        map.put(AbilityEffectId.ENERGY_GRACE, new EnergyGraceStrategy());
        map.put(AbilityEffectId.SHADOW_VOID, new ShadowVoidStrategy());
        
        this.strategies = Collections.unmodifiableMap(map);
    }

    /**
     * Resolves the given ability effect ID into an {@link AbilityEffect}.
     *
     * @param effectId the mapped identifier of the ability effect
     * @return an {@link Optional} containing the matching {@link AbilityEffect}, or
     *         {@link Optional#empty()} when the id is null, NONE, or passive
     *         (handled directly by the pipeline).
     */
    public Optional<AbilityEffect> resolve(final AbilityEffectId effectId) {
        if (effectId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(strategies.get(effectId));
    }
}
