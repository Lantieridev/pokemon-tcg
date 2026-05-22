package ar.edu.utn.frc.tup.piii.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PokemonTcgCardDTO(
        String id,
        String name,
        String supertype,
        List<String> subtypes,
        String hp,
        List<String> rules,
        Object attacks,
        Object weaknesses,
        Object resistances,
        @JsonProperty("retreatCost") List<String> retreatCost,
        PokemonTcgSetDTO set
) {}
