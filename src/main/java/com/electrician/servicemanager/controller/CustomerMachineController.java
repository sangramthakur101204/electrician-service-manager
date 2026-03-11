package com.electrician.servicemanager.controller;

import com.electrician.servicemanager.entity.Customer;
import com.electrician.servicemanager.entity.CustomerMachine;
import com.electrician.servicemanager.repository.CustomerMachineRepository;
import com.electrician.servicemanager.repository.CustomerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customers/{customerId}/machines")
@CrossOrigin(origins = "*")
public class CustomerMachineController {

    private final CustomerMachineRepository machineRepository;
    private final CustomerRepository        customerRepository;

    public CustomerMachineController(CustomerMachineRepository machineRepository,
                                     CustomerRepository customerRepository) {
        this.machineRepository  = machineRepository;
        this.customerRepository = customerRepository;
    }

    /** Get all machines for a customer */
    @GetMapping
    public ResponseEntity<List<CustomerMachine>> getMachines(@PathVariable Long customerId) {
        return ResponseEntity.ok(machineRepository.findByCustomerId(customerId));
    }

    /** Add a machine to a customer */
    @PostMapping
    public ResponseEntity<?> addMachine(@PathVariable Long customerId,
                                        @RequestBody CustomerMachine machine) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) return ResponseEntity.notFound().build();

        if (machine.getMachineType() == null || machine.getMachineType().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Machine type required"));
        if (machine.getMachineBrand() == null || machine.getMachineBrand().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Machine brand required"));

        machine.setCustomer(customer);
        return ResponseEntity.ok(machineRepository.save(machine));
    }

    /** Update a machine */
    @PutMapping("/{machineId}")
    public ResponseEntity<?> updateMachine(@PathVariable Long customerId,
                                           @PathVariable Long machineId,
                                           @RequestBody CustomerMachine updated) {
        return machineRepository.findById(machineId).map(m -> {
            if (!m.getCustomer().getId().equals(customerId))
                return ResponseEntity.badRequest().body(Map.of("error", "Machine does not belong to this customer"));
            m.setMachineType(updated.getMachineType());
            m.setMachineBrand(updated.getMachineBrand());
            m.setModel(updated.getModel());
            m.setSerialNumber(updated.getSerialNumber());
            m.setNotes(updated.getNotes());
            m.setPurchaseDate(updated.getPurchaseDate());
            return ResponseEntity.ok(machineRepository.save(m));
        }).orElse(ResponseEntity.notFound().build());
    }

    /** Delete a machine */
    @DeleteMapping("/{machineId}")
    public ResponseEntity<Void> deleteMachine(@PathVariable Long customerId,
                                              @PathVariable Long machineId) {
        return machineRepository.findById(machineId).map(m -> {
            machineRepository.delete(m);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}