const fs = require('fs');
let c = fs.readFileSync('BE/src/test/java/ar/edu/utn/frc/tup/piii/services/deck/DeckBuilderValidatorTest.java', 'utf8');

c = c.replace(/validator\.validate\(validDeck\(\)/g, 'validator.validate(validDeck(), ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID');
c = c.replace(/validator\.validate\(deck\)/g, 'validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID)');
c = c.replace(/validator\.validate\(Collections\.emptyList\(\)/g, 'validator.validate(Collections.emptyList(), ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID');
c = c.replace(/validator\.validate\(over4\)/g, 'validator.validate(over4, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID)');

fs.writeFileSync('BE/src/test/java/ar/edu/utn/frc/tup/piii/services/deck/DeckBuilderValidatorTest.java', c, 'utf8');
