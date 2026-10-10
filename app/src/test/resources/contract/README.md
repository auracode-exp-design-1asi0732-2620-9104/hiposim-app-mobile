# Ejemplos del contrato de la API

Copias literales de los ejemplos de `docs/api-contract/openapi.v0.yaml` (v0.1.0) del repo `hiposim-backEnd`, con el
cronograma acortado a 3 filas igual que allí. Las pruebas del mapper y del repositorio los usan como respuestas del
servidor.

`lead-response.json` y los `problem-lead-*` siguen `src/HipoSim.Api/Leads` del backend (aún no están en el YAML).
`agencies-response.json` es una forma PROPUESTA: `GET /api/agencies` todavía no existe.

Si el contrato cambia, actualiza estos archivos primero: las pruebas que fallen indican qué parte de la app debe
adaptarse.
