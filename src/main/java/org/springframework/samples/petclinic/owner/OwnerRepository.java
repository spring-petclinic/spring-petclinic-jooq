/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Nonnull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.samples.petclinic.system.JooqHelper;
import org.springframework.samples.petclinic.system.Page;
import org.springframework.samples.petclinic.system.Pageable;
import org.springframework.stereotype.Repository;

import static java.util.Objects.requireNonNull;
import static org.jooq.generated.tables.Owners.OWNERS;
import static org.jooq.generated.tables.Pets.PETS;
import static org.jooq.generated.tables.Types.TYPES;
import static org.jooq.generated.tables.Visits.VISITS;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

/**
 * Repository class for <code>Owner</code> domain objects All method names are compliant
 * with Spring Data naming conventions so this interface can easily be extended for Spring
 * Data. See:
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Wick Dynex
 */
@Repository
public class OwnerRepository {

	public static final Field<List<Pet>> MULTISET_PETS = multiset(
			select().from(PETS).join(PETS.types_()).where(OWNERS.ID.eq(PETS.OWNER_ID)))
		.as("pets")
		.convertFrom(result -> result.map(it -> new Pet(it.get(PETS.ID), it.get(PETS.NAME), it.get(PETS.BIRTH_DATE),
				new PetType(it.get(PETS.TYPE_ID), it.get(TYPES.NAME)))));

	private final DSLContext dsl;

	public OwnerRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	/**
	 * Retrieve {@link Owner}s from the data store by last name, returning all owners
	 * whose last name <i>starts</i> with the given name.
	 * @param lastName Value to search for
	 * @return a Collection of matching {@link Owner}s (or an empty Collection if none
	 * found)
	 */
	public Page<Owner> findByLastNameStartingWith(String lastName, Pageable pageable) {
		var ref = new Object() {
			Integer totalOwners = 0;

		};
		List<Owner> owners = JooqHelper
			.paginate(dsl,
					dsl.select(OWNERS.ID, OWNERS.FIRST_NAME, OWNERS.LAST_NAME, OWNERS.ADDRESS, OWNERS.CITY,
							OWNERS.TELEPHONE, MULTISET_PETS)
						.from(OWNERS)
						.where(OWNERS.LAST_NAME.likeIgnoreCase(lastName + "%")),
					new Field[] { OWNERS.ID }, pageable.pageSize(), pageable.getOffset())
			.fetch(it -> {
				ref.totalOwners = (Integer) it.get("total_rows");
				return new Owner(it.get(OWNERS.ID), it.get(OWNERS.FIRST_NAME), it.get(OWNERS.LAST_NAME),
						it.get(OWNERS.ADDRESS), it.get(OWNERS.CITY), it.get(OWNERS.TELEPHONE), it.get(MULTISET_PETS));
			});
		return new Page<>(owners, pageable, ref.totalOwners);
	}

	/**
	 * Retrieve an {@link Owner} from the data store by id.
	 * <p>
	 * This method returns an {@link Optional} containing the {@link Owner} if found. If
	 * no {@link Owner} is found with the provided id, it will return an empty
	 * {@link Optional}.
	 * </p>
	 * @param id the id to search for
	 * @return an {@link Optional} containing the {@link Owner} if found, or an empty
	 * {@link Optional} if not found.
	 * @throws IllegalArgumentException if the id is null (assuming null is not a valid
	 * input for id)
	 */
	public Optional<Owner> findById(@Nonnull Integer id) {
		Optional<Owner> owner = dsl
			.select(OWNERS.ID, OWNERS.FIRST_NAME, OWNERS.LAST_NAME, OWNERS.ADDRESS, OWNERS.CITY, OWNERS.TELEPHONE,
					MULTISET_PETS)
			.from(OWNERS)
			.where(OWNERS.ID.eq(id))
			.fetchOptional(it -> new Owner(it.get(OWNERS.ID), it.get(OWNERS.FIRST_NAME), it.get(OWNERS.LAST_NAME),
					it.get(OWNERS.ADDRESS), it.get(OWNERS.CITY), it.get(OWNERS.TELEPHONE), it.get(MULTISET_PETS)));

		// TODO : improve the N+1 select issue (not reallu a problem in this use case
		// because an owner has a limited number of pets)
		owner.ifPresent(value -> value.getPets().forEach(pet -> {
			List<Visit> visits = dsl.select(VISITS.ID, VISITS.PET_ID, VISITS.VISIT_DATE, VISITS.DESCRIPTION)
				.from(VISITS)
				.where(VISITS.PET_ID.eq(pet.getId()))
				.fetch(it -> new Visit(it.get(VISITS.ID), it.get(VISITS.VISIT_DATE), it.get(VISITS.DESCRIPTION)));
			pet.getVisits().addAll(visits);
		}));
		return owner;

	}

	public Integer save(Owner owner) {
		if (owner.isNew()) {
			return requireNonNull(
					dsl.insertInto(OWNERS).set(mapOwnerToRecord(owner)).returningResult(OWNERS.ID).fetchOne())
				.getValue(OWNERS.ID);
		}
		else {
			dsl.update(OWNERS).set(mapOwnerToRecord(owner)).where(OWNERS.ID.eq(owner.getId())).execute();
			return owner.getId();
		}
	}

	private Map<Field<?>, Object> mapOwnerToRecord(Owner owner) {
		return Map.of(OWNERS.FIRST_NAME, owner.getFirstName(), OWNERS.LAST_NAME, owner.getLastName(), OWNERS.ADDRESS,
				owner.getAddress(), OWNERS.CITY, owner.getCity(), OWNERS.TELEPHONE, owner.getTelephone());
	}

}
