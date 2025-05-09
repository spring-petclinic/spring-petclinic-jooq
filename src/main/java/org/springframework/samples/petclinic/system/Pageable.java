package org.springframework.samples.petclinic.system;

public record Pageable(int pageNumber, int pageSize) {

	public long getOffset() {
		return (long) pageNumber * (long) pageSize;
	}

	public static Pageable of(int pageNumber, int pageSize) {
		return new Pageable(pageNumber, pageSize);
	}

	public static Pageable ofSize(int pageSize) {
		return new Pageable(0, pageSize);
	}
}
