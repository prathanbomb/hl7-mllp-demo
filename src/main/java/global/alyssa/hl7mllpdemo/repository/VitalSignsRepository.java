package global.alyssa.hl7mllpdemo.repository;

import global.alyssa.hl7mllpdemo.model.VitalSigns;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VitalSignsRepository extends JpaRepository<VitalSigns, Long> {
}