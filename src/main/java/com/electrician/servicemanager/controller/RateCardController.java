package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.RateCard;
import com.electrician.servicemanager.repository.RateCardRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rate-cards")
@CrossOrigin(origins = "*")
public class RateCardController {

    private final RateCardRepository rateCardRepository;

    public RateCardController(RateCardRepository rateCardRepository) {
        this.rateCardRepository = rateCardRepository;
    }

    @PostConstruct
    public void seedDefaultRateCards() {
        if (rateCardRepository.count() > 0) return;

        List<RateCard> defaults = List.of(
                // ── WASHING MACHINE — Top Load Accessories ──────────────────────────────
                rc("Washing Machine", "Wiring Harness Repair",        "Top Load",          175.0,  "per visit"),
                rc("Washing Machine", "Rat Mesh",                     "Top Load",          300.0,  "per visit"),
                rc("Washing Machine", "Chemical Wash - Drum Dismantling","Top Load",        1000.0, "per visit"),
                rc("Washing Machine", "Power Cord",                   "Top Load",          275.0,  "per visit"),
                rc("Washing Machine", "Drain Pipe",                   "Top Load",          225.0,  "per visit"),
                rc("Washing Machine", "Inlet Pipe (with adaptor)",    "Top Load",          450.0,  "per visit"),
                rc("Washing Machine", "Spin Lid",                     "Top Load",          525.0,  "per visit"),
                rc("Washing Machine", "Pulsator with Snap",           "Top Load",          1100.0, "per visit"),
                rc("Washing Machine", "Agitator",                     "Top Load",          800.0,  "per visit"),
                rc("Washing Machine", "Outer Tub",                    "Top Load",          1600.0, "per visit"),
                rc("Washing Machine", "Outer Body / Cabinet",         "Top Load",          2000.0, "per visit"),
                // Top Load Wash Issue
                rc("Washing Machine", "AC Inlet Valve 2 way",         "Top Load - Wash",   950.0,  "per visit"),
                rc("Washing Machine", "DC Inlet Valve 1 way",         "Top Load - Wash",   950.0,  "per visit"),
                rc("Washing Machine", "Main Motor",                   "Top Load - Wash",   1950.0, "per visit"),
                rc("Washing Machine", "Hall Sensor",                  "Top Load - Wash",   875.0,  "per visit"),
                rc("Washing Machine", "Capacitor",                    "Top Load - Wash",   450.0,  "per visit"),
                rc("Washing Machine", "V Belt",                       "Top Load - Wash",   300.0,  "per visit"),
                rc("Washing Machine", "Direct Drive Motor",           "Top Load - Wash",   4000.0, "per visit"),
                rc("Washing Machine", "Wash Timer",                   "Top Load - Wash",   750.0,  "per visit"),
                rc("Washing Machine", "Heater",                       "Top Load - Wash",   725.0,  "per visit"),
                // Top Load Spin Issue
                rc("Washing Machine", "Damper Rod / Balancing Rods",  "Top Load - Spin",   1400.0, "per visit"),
                rc("Washing Machine", "Haier 6.5kg Clutch Assembly",  "Top Load - Spin",   3750.0, "per visit"),
                rc("Washing Machine", "Clutch Assembly",              "Top Load - Spin",   3450.0, "per visit"),
                rc("Washing Machine", "Drain Motor",                  "Top Load - Spin",   1300.0, "per visit"),
                rc("Washing Machine", "Spin Timer",                   "Top Load - Spin",   450.0,  "per visit"),
                // Top Load Power
                rc("Washing Machine", "Inverter PCB Repair Top Load", "Top Load - PCB",    2100.0, "per visit"),
                rc("Washing Machine", "PCB Repair",                   "Top Load - PCB",    1575.0, "per visit"),
                rc("Washing Machine", "Re Wiring (Heavy Damage)",     "Top Load - PCB",    600.0,  "per visit"),
                // Front Load Accessories
                rc("Washing Machine", "Door Hinge",                   "Front Load",        1050.0, "per visit"),
                rc("Washing Machine", "Drain Rubber Hose",            "Front Load",        650.0,  "per visit"),
                rc("Washing Machine", "Noise Filter",                 "Front Load",        500.0,  "per visit"),
                rc("Washing Machine", "Gasket / Door Diaphragm",      "Front Load",        1550.0, "per visit"),
                rc("Washing Machine", "Back Tub (with bearings and seal)","Front Load",     2500.0, "per visit"),
                rc("Washing Machine", "Frontload Trolly (Stand)",     "Front Load",        1250.0, "per visit"),
                // Front Load Wash Issue
                rc("Washing Machine", "Hall Sensor (FL)",             "Front Load - Wash", 1000.0, "per visit"),
                rc("Washing Machine", "Door Lock",                    "Front Load - Wash", 1350.0, "per visit"),
                rc("Washing Machine", "Complete Drum Assembly (Bosch)","Front Load - Wash", 8250.0, "per visit"),
                rc("Washing Machine", "Direct Drive Motor (FL)",      "Front Load - Wash", 3900.0, "per visit"),
                rc("Washing Machine", "Heater (FL)",                  "Front Load - Wash", 1000.0, "per visit"),
                rc("Washing Machine", "V Belt (FL)",                  "Front Load - Wash", 800.0,  "per visit"),
                // Front Load Spin
                rc("Washing Machine", "Drain Pump Motor",             "Front Load - Spin", 1400.0, "per visit"),
                rc("Washing Machine", "Assembly Drain Pump",          "Front Load - Spin", 1350.0, "per visit"),
                rc("Washing Machine", "Drain Pump Motor Double Way",  "Front Load - Spin", 2150.0, "per visit"),
                // Semi WM
                rc("Washing Machine", "Wash Motor (Semi)",            "Semi WM - Wash",    1700.0, "per visit"),
                rc("Washing Machine", "Wash Timer (Semi)",            "Semi WM - Wash",    800.0,  "per visit"),
                rc("Washing Machine", "Gear Box (with Pulley)",       "Semi WM - Wash",    1500.0, "per visit"),
                rc("Washing Machine", "Spin Motor (Semi)",            "Semi WM - Spin",    1850.0, "per visit"),
                rc("Washing Machine", "Coupler / Brake Wheel",        "Semi WM - Spin",    400.0,  "per visit"),
                rc("Washing Machine", "Installation / Un-installation","Semi WM",          100.0,  "per visit"),
                rc("Washing Machine", "Descaling",                    "Semi WM",           200.0,  "per visit"),
                // Minor repairs
                rc("Washing Machine", "Filter Cleaning",              "Minor Repair",      0.0,    "per visit"),
                rc("Washing Machine", "Machine Level Adjustment",     "Minor Repair",      0.0,    "per visit"),
                rc("Washing Machine", "Belt Adjustment",              "Minor Repair",      0.0,    "per visit"),

                // ── WATER PURIFIER ────────────────────────────────────────────────────────
                rc("Water Purifier", "Full Service (1500 TDS)",       "Servicing",         1800.0, "per visit"),
                rc("Water Purifier", "Full Service (8 Nano Filter)",  "Servicing",         2000.0, "per visit"),
                rc("Water Purifier", "Regular Service",               "Servicing",         1000.0, "per visit"),
                rc("Water Purifier", "Sediment Filter (Classic)",     "Consumables",       250.0,  "per piece"),
                rc("Water Purifier", "Spun Filter",                   "Consumables",       220.0,  "per piece"),
                rc("Water Purifier", "Pre Carbon Filter (Classic)",   "Consumables",       250.0,  "per piece"),
                rc("Water Purifier", "RO Membrane 500 TDS",           "Consumables",       875.0,  "per piece"),
                rc("Water Purifier", "RO Membrane <1500 TDS",         "Consumables",       1400.0, "per piece"),
                rc("Water Purifier", "RO Membrane <3000 TDS",         "Consumables",       1950.0, "per piece"),
                rc("Water Purifier", "Booster Pump 100 GPD",          "Consumables",       1900.0, "per piece"),
                rc("Water Purifier", "Booster Pump 75 GPD",           "Consumables",       1750.0, "per piece"),
                rc("Water Purifier", "SMPS (Accord)",                 "Electrical Parts",  750.0,  "per piece"),
                rc("Water Purifier", "PCB Repair",                    "Electrical Parts",  1150.0, "per visit"),
                rc("Water Purifier", "SMPS 24v/36v",                  "Electrical Parts",  650.0,  "per piece"),
                rc("Water Purifier", "UV Lamp 11W 8inch",             "Electrical Parts",  325.0,  "per piece"),
                rc("Water Purifier", "UV Lamp 4W",                    "Electrical Parts",  425.0,  "per piece"),
                rc("Water Purifier", "Solenoid Valve (SLX)",          "Accessories",       225.0,  "per piece"),
                rc("Water Purifier", "Pressure Tank",                 "Accessories",       3200.0, "per piece"),
                rc("Water Purifier", "TDS Controller",                "Accessories",       175.0,  "per piece"),
                rc("Water Purifier", "Alkaline Filter (4 inch)",      "Filters",           425.0,  "per piece"),
                rc("Water Purifier", "Alkaline Filter (10 inch)",     "Filters",           625.0,  "per piece"),
                rc("Water Purifier", "Carbon Block Cartridge",        "Filters",           225.0,  "per piece"),
                rc("Water Purifier", "Installation",                  "Installation",      150.0,  "per visit"),
                rc("Water Purifier", "Uninstallation",                "Installation",      50.0,   "per visit"),
                rc("Water Purifier", "Minor Repair",                  "General",           0.0,    "per visit"),
                // Geyser
                rc("Water Purifier", "Geyser Servicing upto 10L",     "Geyser",            475.0,  "per visit"),
                rc("Water Purifier", "Geyser Servicing 10-25L",       "Geyser",            600.0,  "per visit"),
                rc("Water Purifier", "Heating Element Small <10L",    "Geyser - Parts",    725.0,  "per piece"),
                rc("Water Purifier", "Heating Element Medium 10-30L", "Geyser - Parts",    875.0,  "per piece"),
                rc("Water Purifier", "Copper Tank Small <10L",        "Geyser - Parts",    1950.0, "per piece"),
                rc("Water Purifier", "Geyser Installation",           "Geyser",            450.0,  "per visit"),

                // ── REFRIGERATOR ─────────────────────────────────────────────────────────
                rc("Refrigerator", "Gas Charge (Double Door)",        "Double Door",       1800.0, "per visit"),
                rc("Refrigerator", "Condenser Coil",                  "Double Door",       1200.0, "per piece"),
                rc("Refrigerator", "New Cooling Coil (Double Door)",  "Double Door",       2400.0, "per piece"),
                rc("Refrigerator", "Defrost Heater",                  "Double Door - Defrost", 900.0, "per piece"),
                rc("Refrigerator", "Defrost Timer",                   "Double Door - Defrost", 925.0, "per piece"),
                rc("Refrigerator", "Thermal Fuse",                    "Double Door - Defrost", 200.0, "per piece"),
                rc("Refrigerator", "Room Sensor",                     "Double Door - Defrost", 650.0, "per piece"),
                rc("Refrigerator", "Repair PCB (Double Door)",        "Double Door - PCB", 1500.0, "per visit"),
                rc("Refrigerator", "Repair Inverter PCB",             "Double Door - PCB", 2200.0, "per visit"),
                rc("Refrigerator", "Compressor <500L (Double Door)",  "Double Door - Compressor", 5350.0, "per visit"),
                rc("Refrigerator", "Inverter Compressor (Double Door)","Double Door - Compressor", 7250.0, "per visit"),
                rc("Refrigerator", "ReF Door Gasket <400L",           "Double Door - Accessories", 1650.0, "per piece"),
                rc("Refrigerator", "ReF Door Gasket >400L",           "Double Door - Accessories", 2100.0, "per piece"),
                rc("Refrigerator", "Fan Blade",                       "Double Door - Others", 110.0, "per piece"),
                rc("Refrigerator", "LED Light",                       "Double Door - Others", 260.0, "per piece"),
                rc("Refrigerator", "Duct and Drain Cleaning",         "Double Door - Others", 250.0, "per visit"),
                // Single Door
                rc("Refrigerator", "Gas Charge (Single Door)",        "Single Door",       1400.0, "per visit"),
                rc("Refrigerator", "Cooling Coil (Single Door)",      "Single Door - Gas", 1350.0, "per piece"),
                rc("Refrigerator", "Compressor (Single Door)",        "Single Door - Compressor", 3200.0, "per visit"),
                rc("Refrigerator", "Inverter Compressor (Single Door)","Single Door - Compressor", 6400.0, "per visit"),
                rc("Refrigerator", "Repair PCB (Single Door)",        "Single Door - PCB", 1400.0, "per visit"),
                rc("Refrigerator", "Thermostat",                      "Single Door",       690.0,  "per piece"),
                rc("Refrigerator", "Defrost Sensor",                  "Single Door",       450.0,  "per piece"),
                // Side by Side
                rc("Refrigerator", "Gas Charge (Side-by-Side)",       "Side-by-Side",      2250.0, "per visit"),
                rc("Refrigerator", "Compressor (Side-by-Side)",       "Side-by-Side - Compressor", 6450.0, "per visit"),
                rc("Refrigerator", "Repair Inverter PCB (SBS)",       "Side-by-Side - PCB", 2800.0, "per visit"),
                rc("Refrigerator", "Water Tank or Pipe",              "Side-by-Side",      1350.0, "per piece"),
                rc("Refrigerator", "Ice Maker Kit",                   "Side-by-Side",      2400.0, "per piece"),
                // Minor
                rc("Refrigerator", "Refrigerator Levelling",          "Minor Repair",      0.0,    "per visit"),
                rc("Refrigerator", "Door-gap Fix",                    "Minor Repair",      0.0,    "per visit"),
                rc("Refrigerator", "Temperature Setting",             "Minor Repair",      0.0,    "per visit"),

                // ── MICROWAVE ────────────────────────────────────────────────────────────
                rc("Microwave", "Magnetron 4 Fins",                   "No Heating",        1500.0, "per piece"),
                rc("Microwave", "Magnetron 6 Fins",                   "No Heating",        1700.0, "per piece"),
                rc("Microwave", "Magnetron 5 Fins",                   "No Heating",        1600.0, "per piece"),
                rc("Microwave", "HV Capacitor",                       "No Heating",        625.0,  "per piece"),
                rc("Microwave", "Grill Heater",                       "No Heating",        725.0,  "per piece"),
                rc("Microwave", "Smart Oven Grill Heater",            "No Heating",        1500.0, "per piece"),
                rc("Microwave", "HV Diode",                           "No Heating",        350.0,  "per piece"),
                rc("Microwave", "HV Transformer",                     "No Heating",        1700.0, "per piece"),
                rc("Microwave", "Convection Heater",                  "No Heating",        760.0,  "per piece"),
                rc("Microwave", "Turn Table Motor",                   "Noise Issue",       650.0,  "per piece"),
                rc("Microwave", "Roller",                             "Noise Issue",       250.0,  "per piece"),
                rc("Microwave", "Fan Motor",                          "Noise Issue",       800.0,  "per piece"),
                rc("Microwave", "Door Latch",                         "Accessories",       600.0,  "per piece"),
                rc("Microwave", "Glass Turn Table",                   "Accessories",       750.0,  "per piece"),
                rc("Microwave", "Door Switch",                        "Accessories",       375.0,  "per piece"),
                rc("Microwave", "Coupler",                            "Accessories",       225.0,  "per piece"),
                rc("Microwave", "Power 3 Pin Plug Top",               "Accessories",       160.0,  "per piece"),
                rc("Microwave", "Mica Sheet",                         "Accessories",       250.0,  "per piece"),
                rc("Microwave", "PCB New",                            "Power Unit",        2300.0, "per piece"),
                rc("Microwave", "PCB Repair",                         "Power Unit",        1400.0, "per visit"),
                rc("Microwave", "Switch Membrane / Keypad Repair",    "Power Unit",        1450.0, "per visit"),
                rc("Microwave", "Timer",                              "Power Unit",        950.0,  "per piece"),
                rc("Microwave", "Thermal Cut Off (TCO)",              "Power Unit",        350.0,  "per piece"),
                rc("Microwave", "Cavity Cleaning",                    "Minor Repair",      0.0,    "per visit"),
                rc("Microwave", "Electrical Plug Adjustment",         "Minor Repair",      0.0,    "per visit"),
                rc("Microwave", "Ventilation Clean",                  "Minor Repair",      0.0,    "per visit"),

                // ── GENERAL ──────────────────────────────────────────────────────────────
                rc("General", "Visit / Inspection Charge",            "All Appliances",    200.0,  "per visit")
        );

        rateCardRepository.saveAll(defaults);
        System.out.println("[RateCard] Seeded: " + defaults.size() + " entries from Matoshree Enterprises rate cards.");
    }

    private RateCard rc(String cat, String name, String desc, Double price, String unit) {
        RateCard r = new RateCard();
        r.setCategory(cat);
        r.setServiceName(name);
        r.setDescription(desc);
        r.setPrice(price);
        r.setUnit(unit);
        r.setIsActive(true);
        return r;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<RateCard> add(@RequestBody RateCard rateCard) {
        rateCard.setIsActive(true);
        return ResponseEntity.ok(rateCardRepository.save(rateCard));
    }

    @GetMapping
    public ResponseEntity<List<RateCard>> getAll() {
        return ResponseEntity.ok(rateCardRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<RateCard>> getActive() {
        return ResponseEntity.ok(rateCardRepository.findByIsActiveTrue());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<RateCard>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(rateCardRepository.findByCategoryAndIsActiveTrue(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RateCard> update(@PathVariable Long id, @RequestBody RateCard updated) {
        return rateCardRepository.findById(id).map(r -> {
            r.setCategory(updated.getCategory());
            r.setServiceName(updated.getServiceName());
            r.setDescription(updated.getDescription());
            r.setPrice(updated.getPrice());
            r.setUnit(updated.getUnit());
            r.setIsActive(updated.getIsActive());
            return ResponseEntity.ok(rateCardRepository.save(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!rateCardRepository.existsById(id)) return ResponseEntity.notFound().build();
        rateCardRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}