package ar.edu.utn.frc.tup.piii.services.persistence;

import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;

public record SaveMatchEvent(MatchSession session) {}
