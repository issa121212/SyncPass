"""Comprobación optativa: solo el servidor local con identidades ficticias del ensayo."""
import json
import re
import uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.request import Request, urlopen


def main():
    base = "http://127.0.0.1:18081/api"
    with urlopen("http://127.0.0.1:18082/tokens", timeout=10) as response:
        token = json.load(response)["teacher"]

    def call(method, path, body):
        request = Request(base + path, data=json.dumps(body).encode(), method=method,
                          headers={"Authorization": "Bearer " + token, "Content-Type": "application/json"})
        with urlopen(request, timeout=20) as response:
            return json.load(response)

    projects = []
    for _ in range(2):
        body = {"nombre": "Ensayo mismo nombre", "clientUuid": str(uuid.uuid4()),
                "codigoPublico": "PRY-000000000000"}
        project = call("POST", "/proyectos", body)
        assert re.fullmatch(r"PRY-[0-9A-F]{12}", project["codigoPublico"])
        assert project["codigoPublico"] != body["codigoPublico"]
        assert call("POST", "/proyectos", body)["codigoPublico"] == project["codigoPublico"]
        projects.append(project)
    assert projects[0]["codigoPublico"] != projects[1]["codigoPublico"]
    updated = call("PUT", "/proyectos/" + str(projects[0]["id"]), {
        "nombre": "Nombre corregido", "codigoPublico": projects[1]["codigoPublico"]})
    assert updated["codigoPublico"] == projects[0]["codigoPublico"]

    events = []
    for project in projects:
        body = {"proyectoId": project["id"], "nombre": "Ensayo evento repetido",
                "fechaHora": "2026-12-05T14:30:00", "clientUuid": str(uuid.uuid4())}
        with ThreadPoolExecutor(max_workers=2) as pool:
            replies = list(pool.map(lambda _: call("POST", "/eventos", body), range(2)))
        assert replies[0]["id"] == replies[1]["id"]
        assert replies[0]["codigoPublico"] == replies[1]["codigoPublico"]
        event = replies[0]
        assert re.fullmatch(r"EVT-[0-9A-F]{12}", event["codigoPublico"])
        assert event["proyectoId"] == project["id"]
        updated = call("PUT", "/eventos/" + str(event["id"]), {
            **body, "nombre": "Ensayo evento editado", "codigoPublico": "EVT-000000000000"})
        assert updated["codigoPublico"] == event["codigoPublico"]
        assert updated["qrContenido"] == event["qrContenido"]
        events.append(event)
    assert events[0]["codigoPublico"] != events[1]["codigoPublico"]
    report = ("API LOCAL: proyectos y eventos homónimos con códigos distintos.\n"
              "Edición conserva códigos y QR; código de entrada ignorado.\n"
              "Reintentos concurrentes devuelven mismo ID y código; relaciones preservadas.\n")
    output = Path("build/migration-rehearsal/codigos-publicos-api.txt")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(report, encoding="utf-8")
    print(report)


if __name__ == "__main__":
    main()
