package com.duoc.cursos.service;

import com.duoc.cursos.dto.ApiDtos.AsistenciaRequest;
import com.duoc.cursos.dto.ApiDtos.AsistenciaSyncItem;
import com.duoc.cursos.dto.ApiDtos.EncuestaRequest;
import com.duoc.cursos.dto.ApiDtos.EncuestaSyncItem;
import com.duoc.cursos.dto.ApiDtos.SyncResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SyncService {

    private final EventoService eventoService;
    private final EncuestaService encuestaService;

    public SyncService(EventoService eventoService, EncuestaService encuestaService) {
        this.eventoService = eventoService;
        this.encuestaService = encuestaService;
    }

    public SyncResult sincronizarAsistencias(List<AsistenciaSyncItem> items) {
        int procesados = 0;
        List<String> mensajes = new ArrayList<>();

        for (AsistenciaSyncItem item : items) {
            try {
                eventoService.escanear(item.eventoId(), new AsistenciaRequest(
                        item.codigoQr(),
                        true,
                        item.marcadoEn(),
                        item.latitud(),
                        item.longitud(),
                        item.precisionMetros(),
                        item.clientUuid()
                ));
                procesados++;
            } catch (Exception ex) {
                mensajes.add("Evento " + item.eventoId() + ": " + "No se pudo procesar la operacion");
            }
        }
        return new SyncResult(procesados, mensajes.size(), mensajes);
    }

    public SyncResult sincronizarEncuestas(List<EncuestaSyncItem> items) {
        int procesados = 0;
        List<String> mensajes = new ArrayList<>();

        for (EncuestaSyncItem item : items) {
            try {
                encuestaService.guardarPropia(item.eventoId(), new EncuestaRequest(
                        item.satisfaccion(),
                        item.utilidad(),
                        item.comentarios(),
                        item.enviadoEn()
                ));
                procesados++;
            } catch (Exception ex) {
                mensajes.add("Evento " + item.eventoId() + ": " + "No se pudo procesar la operacion");
            }
        }
        return new SyncResult(procesados, mensajes.size(), mensajes);
    }
}
