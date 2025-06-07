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
package org.springframework.samples.petclinic.vet;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.exception.DataAccessException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.samples.petclinic.system.JooqHelper;
import org.springframework.samples.petclinic.system.Page;
import org.springframework.samples.petclinic.system.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static org.jooq.generated.tables.Specialties.SPECIALTIES;
import static org.jooq.generated.tables.VetSpecialties.*;
import static org.jooq.generated.tables.Vets.*;
import static org.jooq.impl.DSL.*;

/**
 * Repository class for <code>Vet</code> domain objects. <p N relationships can be managed
 * with multiple SQL queries, but standard SQL allows nesting queries using the MULTISET
 * function.
 * </p>
 *
 * @see <a href=
 * "https://www.jooq.org/doc/latest/manual/sql-building/column-expressions/multiset-value-constructor/">MULTISET
 * value constructor</a>
 * @see <a href=
 * "https://blog.jooq.org/jooq-3-15s-new-multiset-operator-will-change-how-you-think-about-sql/">jOOQ
 * 3.15’s New Multiset Operator Will Change How You Think About SQL</a>
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 */
@Repository
public class VetRepository {

	public static final Field<List<Specialty>> MULTISET_SPECIALITIES = multiset(
			select(VET_SPECIALTIES.specialties().ID, VET_SPECIALTIES.specialties().NAME).from(VET_SPECIALTIES)
				.where(VET_SPECIALTIES.VET_ID.eq(VETS.ID)))
		.as("specialties")
		.convertFrom(result -> result.map(it -> new Specialty(it.get(SPECIALTIES.ID), it.get(SPECIALTIES.NAME))));

	private final DSLContext dsl;

	public VetRepository(DSLContext dslContext) {
		this.dsl = dslContext;
	}

	/**
	 * Retrieve all <code>Vet</code>s from the data store.
	 * @return a <code>Collection</code> of <code>Vet</code>s
	 */
	@Transactional(readOnly = true)
	@Cacheable("vets")
	public List<Vet> findAll() throws DataAccessException {
		return dsl.select(VETS.ID, VETS.FIRST_NAME, VETS.LAST_NAME, MULTISET_SPECIALITIES)
			.from(VETS)
			.leftJoin(VETS.vetSpecialties())
			.orderBy(VETS.ID)
			.fetch(VetRepository::toVet);
	}

	private static Vet toVet(Record4<Integer, String, String, List<Specialty>> row) {
		return new Vet(row.get(VETS.ID), row.get(VETS.FIRST_NAME), row.get(VETS.LAST_NAME),
				new HashSet<>(row.get(MULTISET_SPECIALITIES)));
	}

	/**
	 * Retrieve all <code>Vet</code>s from data store in Pages
	 * @param pageable
	 * @return
	 * @throws DataAccessException
	 */
	@Transactional(readOnly = true)
	@Cacheable("vets")
	public Page<Vet> findAll(Pageable pageable) throws DataAccessException {
		var ref = new Object() {
			Integer totalVets = 0;

		};
		List<Vet> vets = JooqHelper
			.paginate(dsl, dsl.select(VETS.ID, VETS.FIRST_NAME, VETS.LAST_NAME, MULTISET_SPECIALITIES).from(VETS),
					new Field[] { VETS.ID }, pageable.pageSize(), pageable.getOffset())
			.fetch(it -> {
				ref.totalVets = (Integer) it.get("total_rows");
				return toVet(it);
			});
		return new Page<>(vets, pageable, ref.totalVets);
	}

	private static Vet toVet(org.jooq.Record row) {
		return new Vet(row.get(VETS.ID), row.get(VETS.FIRST_NAME), row.get(VETS.LAST_NAME),
				new HashSet<>(row.get(MULTISET_SPECIALITIES)));
	}

}
