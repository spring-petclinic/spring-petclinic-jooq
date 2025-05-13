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

import java.io.Serializable;
import java.util.*;

import jakarta.xml.bind.annotation.XmlElement;

/**
 * Simple JavaBean domain object representing a veterinarian.
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Arjen Poutsma
 */
public record Vet(Integer id, String firstName, String lastName, Set<Specialty> specialties) implements Serializable {

	public Vet(Integer id, String firstName, String lastName) {
		this(id, firstName, lastName, new HashSet<>());
	}

	@XmlElement
	public List<Specialty> getSpecialties() {
		return specialties.stream().sorted(Comparator.comparing(Specialty::name)).toList();
	}

	public int getNrOfSpecialties() {
		return specialties.size();
	}

	public void addSpecialty(Specialty specialty) {
		specialties.add(specialty);
	}
}
