package com.spaceproject.components;

public class TagComponent {
    //todo: apply tags to entities, this is an identifier. eg:
    //  character = ["character"]
    //  your character ["player", "character"]
    //  ai character = ["ai", "character"]
    //  you piloting = ["player", "ship"] transfer tag component when enter/exit
    //  ai piloting ship = ["ai", "ship] transfer tag component when enter/exit
    //  ship with no pilot = ["ship"]
    //  planet = ["planet", "<type>"] //type? eg: ["Desert", "Ice", "Ocean", "Lava"]
    //  rogue planet = ["planet", "<type>", "rogue"]
    //  star = ["star", "<type>"] //type? eg: [SuperGiants, Giants, White dwarf, Brown dwarf]
    
    // Then in debug engine view, if has entity has tag display it.
    // and display in minmap on mouse over/highlight
    String tags[];
    
}
