package org.springframework.samples.petclinic.owner;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.jooq.generated.Tables.VISITS;

@Repository
public class VisitRepository {

	private final DSLContext dsl;

	public VisitRepository(DSLContext dslContext) {
		this.dsl = dslContext;
	}

	public int save(Visit visit) {
		return dsl.insertInto(VISITS)
			.set(VISITS.PET_ID, visit.petId())
			.set(VISITS.VISIT_DATE, visit.date())
			.set(VISITS.DESCRIPTION, visit.description())
			.execute();
	}

	public List<Visit> findByPetId(int petId) {
		return dsl.selectFrom(VISITS)
			.where(VISITS.PET_ID.eq(petId))
			.orderBy(VISITS.VISIT_DATE.desc())
			.fetch(record -> new Visit(record.get(VISITS.ID), record.get(VISITS.VISIT_DATE),
					record.get(VISITS.DESCRIPTION), record.get(VISITS.PET_ID)));
	}

}
