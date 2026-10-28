package com.rojas.syncpass.data.mapper

import com.rojas.syncpass.data.local.*

import com.rojas.syncpass.data.remote.*

import com.rojas.syncpass.domain.model.*

fun ProyectoEntity.toDomain() = Proyecto(id = id, nombre = nombre, descripcion = descripcion, areaTematica = areaTematica, unidadVecinal = unidadVecinal, fechaInicio = fechaInicio, fechaFin = fechaFin, estado = estado, responsableNombre = responsableNombre, responsableEmail = responsableEmail, responsableId = responsableId)

fun Proyecto.toEntity() = ProyectoEntity(id = id, nombre = nombre, descripcion = descripcion, areaTematica = areaTematica, unidadVecinal = unidadVecinal, fechaInicio = fechaInicio, fechaFin = fechaFin, estado = estado, responsableNombre = responsableNombre, responsableEmail = responsableEmail, responsableId = responsableId)

fun EventoEntity.toDomain() = Evento(id = id, proyectoId = proyectoId, proyectoNombre = proyectoNombre, nombre = nombre, descripcion = descripcion, fechaHora = fechaHora, cupos = cupos, inscritos = inscritos, presentes = presentes, recinto = recinto, direccion = direccion, latitud = latitud, longitud = longitud, radioMetros = radioMetros, qrContenido = qrContenido, estado = estado, inscrito = inscrito, esPresente = esPresente, encuestaRespondida = encuestaRespondida, actualizadoEn = actualizadoEn)

fun Evento.toEntity() = EventoEntity(id = id, proyectoId = proyectoId, proyectoNombre = proyectoNombre, nombre = nombre, descripcion = descripcion, fechaHora = fechaHora, cupos = cupos, inscritos = inscritos, presentes = presentes, recinto = recinto, direccion = direccion, latitud = latitud, longitud = longitud, radioMetros = radioMetros, qrContenido = qrContenido, estado = estado, inscrito = inscrito, esPresente = esPresente, encuestaRespondida = encuestaRespondida, actualizadoEn = actualizadoEn)

fun PerfilEntity.toDomain() = Perfil(id = id, usuarioId = usuarioId, nombre = nombre, email = email, roles = roles, qrContenido = qrContenido)

fun Perfil.toEntity() = PerfilEntity(id = id, usuarioId = usuarioId, nombre = nombre, email = email, roles = roles, qrContenido = qrContenido)

fun RecintoEntity.toDomain() = Recinto(id = id, nombre = nombre, direccion = direccion, latitud = latitud, longitud = longitud, radioMetros = radioMetros)

fun Recinto.toEntity() = RecintoEntity(id = id, nombre = nombre, direccion = direccion, latitud = latitud, longitud = longitud, radioMetros = radioMetros)

fun ProyectoInput.toRequest() = ProyectoRequest(nombre = nombre, descripcion = descripcion, areaTematica = areaTematica, unidadVecinal = unidadVecinal, fechaInicio = fechaInicio, fechaFin = fechaFin, estado = estado)

fun EventoInput.toRequest() = EventoRequest(proyectoId = proyectoId, nombre = nombre, descripcion = descripcion, fechaHora = fechaHora, cupos = cupos, recinto = recinto, direccion = direccion, latitud = latitud, longitud = longitud, radioMetros = radioMetros, estado = estado)

fun AsistenteResponse.toDomain() = Asistente(usuarioId = usuarioId, nombre = nombre, email = email, inscrito = inscrito, presente = presente, marcadoEn = marcadoEn, encuesta = encuesta?.toDomain())

fun EncuestaEventoResponse.toDomain() = EncuestaEvento(id = id, eventoId = eventoId, estudiante = estudiante?.toDomain(), satisfaccion = satisfaccion, utilidad = utilidad, comentarios = comentarios, enviadoEn = enviadoEn)

fun ResponsableResponse.toDomain() = Persona(id = id, nombre = nombre, email = email)

fun EncuestaResponse.toDomain() = Encuesta(id = id, satisfaccion = satisfaccion, utilidad = utilidad, comentarios = comentarios, enviadoEn = enviadoEn)

fun ProyectoResponse.toEntity() = ProyectoEntity(

        id = id,

        nombre = nombre,

        descripcion = descripcion,

        areaTematica = areaTematica,

        unidadVecinal = unidadVecinal,

        fechaInicio = fechaInicio,

        fechaFin = fechaFin,

        estado = estado,

        responsableNombre = responsable?.nombre,

        responsableEmail = responsable?.email,
        responsableId = responsable?.id,

    )

fun EventoResponse.toEntity() = EventoEntity(

        id = id,

        proyectoId = proyectoId,

        proyectoNombre = proyectoNombre,

        nombre = nombre,

        descripcion = descripcion,

        fechaHora = fechaHora,

        cupos = cupos,

        inscritos = inscritos,

        presentes = presentes,

        recinto = recinto,

        direccion = direccion,

        latitud = latitud,

        longitud = longitud,

        radioMetros = radioMetros,

        qrContenido = qrContenido,

        estado = estado,

        inscrito = inscrito,

        esPresente = esPresente,

        encuestaRespondida = encuestaRespondida,

        actualizadoEn = actualizadoEn,

    )


