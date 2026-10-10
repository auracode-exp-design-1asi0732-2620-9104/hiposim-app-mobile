# Datos de paridad con el motor real

`fake-vs-engine-cases.json` guarda 11 casos de `POST /api/simulations` calculados por el motor financiero real de HipoSim
(repositorio `hiposim-backEnd`, proyecto `HipoSim.FinancialEngine`). `request` es lo que se envía y `expected` son las
cifras que devolvió el motor. Todos usan la TEA de referencia de 8.85% (0.0885), con y sin bono y en los bordes de
cada tramo del bono.

`FakeSimulationParityTest` comprueba que `FakeSimulationService` reproduce esas cifras. Importa porque la app usa
el servicio falso por defecto (`USE_FAKE_API`), así que sus resultados deben ser los del motor.

Se generaron el 2026-10-09 contra `develop` de `hiposim-backEnd` (commit `f87bf90`). Solo se guardan las cifras
principales y la primera y la última fila del cronograma, que bastan para detectar una diferencia de fórmula o de redondeo.

## Cómo regenerarlos

Solo hace falta si el motor cambia a propósito (otra fórmula, otra tabla del bono). Con la API local corriendo:

```powershell
python -I make_engine_parity.py http://localhost:5000 fake-vs-engine-cases.json
```

Revisa el cambio con `git diff` antes de aceptarlo: una diferencia en el archivo es una diferencia en el motor.
