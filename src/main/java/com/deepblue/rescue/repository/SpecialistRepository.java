package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Specialist;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpecialistRepository extends JpaRepository<Specialist, Long> {

    @Query("""

            select distinct s

            from Specialist s

            join s.expertiseAreas e

            where lower(e.name) = lower(:expertise)

              and s.active = true

            order by s.lastName, s.firstName

            """)

    List<Specialist> findActiveByExpertise(@Param("expertise") String expertise);

}

