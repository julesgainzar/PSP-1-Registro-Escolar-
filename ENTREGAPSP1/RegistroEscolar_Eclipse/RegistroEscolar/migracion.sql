-- Ejecutar UNA sola vez sobre GESTIONESCOLAR.db (haz una copia antes).
-- Motivo: la app permite dejar un curso sin profesor, pero grupo.id_profesor
-- estaba definido como NOT NULL. SQLite no permite quitar ese NOT NULL con
-- ALTER, así que se reconstruye la tabla conservando todos los datos.
PRAGMA foreign_keys = OFF;
BEGIN;

CREATE TABLE grupo_nuevo (
    id_grupo INTEGER PRIMARY KEY AUTOINCREMENT,
    id_curso INTEGER NOT NULL,
    id_periodo INTEGER NOT NULL,
    id_profesor INTEGER,
    numero_grupo TEXT NOT NULL,
    FOREIGN KEY (id_curso) REFERENCES curso(id_curso),
    FOREIGN KEY (id_periodo) REFERENCES periodo(id_periodo),
    FOREIGN KEY (id_profesor) REFERENCES profesor(id_profesor)
);

INSERT INTO grupo_nuevo (id_grupo, id_curso, id_periodo, id_profesor, numero_grupo)
SELECT id_grupo, id_curso, id_periodo, id_profesor, numero_grupo FROM grupo;

DROP TABLE grupo;
ALTER TABLE grupo_nuevo RENAME TO grupo;

COMMIT;
PRAGMA foreign_keys = ON;
