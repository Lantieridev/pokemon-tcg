import requests
import json
import os

url = "https://api.pokemontcg.io/v2/cards?q=set.id:xy1"
response = requests.get(url)

if response.status_code == 200:
    data = response.json()["data"]
    
    # Ensure directory exists
    os.makedirs("src/main/resources/db/migration", exist_ok=True)
    
    with open("src/main/resources/db/migration/V2__seed_xy1_cards.sql", "w", encoding="utf-8") as f:
        f.write("-- Seed data for Pokemon TCG XY1 Set\n\n")
        f.write("INSERT INTO cards (id, name, supertype, subtype, hp, rules, attacks, weaknesses, resistances, retreat_cost, set_id) VALUES\n")
        
        values_list = []
        for card in data:
            c_id = card.get("id", "")
            c_name = card.get("name", "").replace("'", "''")
            c_supertype = card.get("supertype", "")
            
            subtypes = card.get("subtypes", [])
            c_subtype = ", ".join(subtypes) if subtypes else ""
            
            hp_str = card.get("hp", "0")
            c_hp = int(hp_str) if hp_str.isdigit() else 0
            
            rules = card.get("rules", [])
            c_rules = f"$${json.dumps(rules, ensure_ascii=False)}$$" if rules else "'[]'"
            
            attacks = card.get("attacks", [])
            c_attacks = f"$${json.dumps(attacks, ensure_ascii=False)}$$" if attacks else "'[]'"
            
            weaknesses = card.get("weaknesses", [])
            c_weaknesses = f"$${json.dumps(weaknesses, ensure_ascii=False)}$$" if weaknesses else "'[]'"
            
            resistances = card.get("resistances", [])
            c_resistances = f"$${json.dumps(resistances, ensure_ascii=False)}$$" if resistances else "'[]'"
            
            retreat_cost = card.get("retreatCost", [])
            c_retreat_cost = f"$${json.dumps(retreat_cost, ensure_ascii=False)}$$" if retreat_cost else "'[]'"
            
            c_set_id = "xy1"
            
            val = f"('{c_id}', '{c_name}', '{c_supertype}', '{c_subtype}', {c_hp}, {c_rules}, {c_attacks}, {c_weaknesses}, {c_resistances}, {c_retreat_cost}, '{c_set_id}')"
            values_list.append(val)
            
        f.write(",\n".join(values_list))
        f.write(";\n")
    print("V2__seed_xy1_cards.sql generated successfully with {} cards.".format(len(data)))
else:
    print(f"Failed to fetch cards. Status code: {response.status_code}")
