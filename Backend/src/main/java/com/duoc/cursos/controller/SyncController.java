package com.duoc.cursos.controller;

import com.duoc.cursos.dto.ApiDtos.AsistenciaSyncItem;
import com.duoc.cursos.dto.ApiDtos.EncuestaSyncItem;
import com.duoc.cursos.dto.ApiDtos.SyncResult;
import com.duoc.cursos.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/asistencias")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN','VOLUNTARIO')")
    public SyncResult asistencias(@jakarta.validation.constraints.Size(min = 1, max = 100) @RequestBody List<@Valid AsistenciaSyncItem> items) {
        return syncService.sincronizarAsistencias(items);
    }

    @PostMapping("/encuestas")
    @PreAuthorize("hasAnyRole('PARTICIPANTE','ESTUDIANTE')")
    public SyncResult encuestas(@jakarta.validation.constraints.Size(min = 1, max = 100) @RequestBody List<@Valid EncuestaSyncItem> items) {
        return syncService.sincronizarEncuestas(items);
    }
}
