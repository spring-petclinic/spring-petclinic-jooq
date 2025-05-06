package org.springframework.samples.petclinic.owner;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.jooq.generated.tables.Pets.PETS;
import static org.jooq.generated.tables.Types.TYPES;

@Repository
public class PetRepository {

	private final DSLContext dsl;

	public PetRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	/**
	 * Retrieve all {@link PetType}s from the data store.
	 * @return a Collection of {@link PetType}s.
	 */
	@Transactional(readOnly = true)
	public List<PetType> findPetTypes() {
		return dsl.selectFrom(TYPES).orderBy(TYPES.NAME).fetchInto(PetType.class);
	}

	public void save(Integer ownerId, Pet pet) {
		dsl.insertInto(PETS)
			.set(PETS.NAME, pet.getName())
			.set(PETS.TYPE_ID, pet.getType().getId())
			.set(PETS.BIRTH_DATE, pet.getBirthDate())
			.set(PETS.OWNER_ID, ownerId)
			.execute();
	}

	public void update(Pet pet) {
		dsl.update(PETS)
			.set(PETS.NAME, pet.getName())
			.set(PETS.TYPE_ID, pet.getType().getId())
			.set(PETS.BIRTH_DATE, pet.getBirthDate())
			.where(PETS.ID.eq(pet.getId()))
			.execute();
	}

}
