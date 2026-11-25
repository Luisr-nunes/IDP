package br.com.mentoria.validacao.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidacaoRegistroRepository extends JpaRepository<ValidacaoRegistro, Long> {

    List<ValidacaoRegistro> findTop20ByOrderByCriadoEmDescIdDesc();

    List<ValidacaoRegistro> findAllByOrderByCriadoEmDescIdDesc();
}
