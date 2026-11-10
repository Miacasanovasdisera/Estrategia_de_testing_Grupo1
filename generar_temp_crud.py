from docx import Document
from docx.shared import Inches, Pt
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

def set_table_borders(table, color="BDBDBD", sz="4"):
    tblPr = table._tbl.tblPr
    borders = parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'<w:top w:val="single" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:left w:val="single" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:bottom w:val="single" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:right w:val="single" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:insideH w:val="single" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:insideV w:val="single" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'</w:tblBorders>'
    )
    tblPr.append(borders)

def set_cell_margins(cell, top=100, bottom=100, left=140, right=140):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="{top}" w:type="dxa"/>'
        f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
        f'<w:left w:w="{left}" w:type="dxa"/>'
        f'<w:right w:w="{right}" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tcPr.append(tcMar)

def add_table(doc, headers, rows, col_widths, bold_first_col=False):
    table = doc.add_table(rows=len(rows) + 1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    set_table_borders(table)

    for c_idx, h_text in enumerate(headers):
        cell = table.cell(0, c_idx)
        cell.width = Inches(col_widths[c_idx])
        set_cell_margins(cell)
        p = cell.paragraphs[0]
        p.paragraph_format.space_before = Pt(3)
        p.paragraph_format.space_after = Pt(3)
        run = p.add_run(h_text)
        run.font.name = 'Arial'
        run.font.size = Pt(10.5)
        run.font.bold = False

    for r_idx, row in enumerate(rows):
        for c_idx, val in enumerate(row):
            cell = table.cell(r_idx + 1, c_idx)
            cell.width = Inches(col_widths[c_idx])
            set_cell_margins(cell)
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(3)
            p.paragraph_format.space_after = Pt(3)
            run = p.add_run(val)
            run.font.name = 'Arial'
            run.font.size = Pt(10.5)
            if bold_first_col and c_idx == 0:
                run.font.bold = True

    sp = doc.add_paragraph()
    sp.paragraph_format.space_before = Pt(4)
    sp.paragraph_format.space_after = Pt(4)

def add_method_block(doc, signature, escenario_text, part_rows, bat_rows, bva_subtitle=None):
    p_sig = doc.add_paragraph(style='Heading 3')
    p_sig.paragraph_format.space_before = Pt(12)
    p_sig.paragraph_format.space_after = Pt(4)
    r_sig = p_sig.add_run(signature)
    r_sig.font.name = 'Arial'
    r_sig.font.size = Pt(14)
    r_sig.font.italic = True
    r_sig.font.bold = False

    p_esc = doc.add_paragraph()
    p_esc.paragraph_format.space_before = Pt(2)
    p_esc.paragraph_format.space_after = Pt(4)
    r_esc = p_esc.add_run(escenario_text)
    r_esc.font.name = 'Arial'
    r_esc.font.size = Pt(11)

    p_tp = doc.add_paragraph(style='Heading 4')
    p_tp.paragraph_format.space_before = Pt(4)
    p_tp.paragraph_format.space_after = Pt(2 if bva_subtitle else 4)
    r_tp = p_tp.add_run("Tabla de Particiones")
    r_tp.font.name = 'Arial'
    r_tp.font.size = Pt(12)
    r_tp.font.italic = True
    r_tp.font.bold = False

    if bva_subtitle:
        p_bva = doc.add_paragraph()
        p_bva.paragraph_format.space_before = Pt(0)
        p_bva.paragraph_format.space_after = Pt(4)
        r_bva = p_bva.add_run(bva_subtitle)
        r_bva.font.name = 'Arial'
        r_bva.font.size = Pt(11)

    add_table(
        doc,
        headers=["Condición de entrada", "Clases Válidas", "Clases Inválidas"],
        rows=part_rows,
        col_widths=[1.8, 2.35, 2.35]
    )

    p_bp = doc.add_paragraph(style='Heading 4')
    p_bp.paragraph_format.space_before = Pt(4)
    p_bp.paragraph_format.space_after = Pt(4)
    r_bp = p_bp.add_run("Batería de Pruebas")
    r_bp.font.name = 'Arial'
    r_bp.font.size = Pt(12)
    r_bp.font.italic = True
    r_bp.font.bold = False

    add_table(
        doc,
        headers=["Tipo de clase", "Invocación / Parámetros", "Salida esperada", "Clases cubiertas", "Método @Test Asociado"],
        rows=bat_rows,
        col_widths=[1.05, 1.55, 1.85, 0.75, 1.3]
    )

def build_docx(filename):
    doc = Document()
    for s in doc.sections:
        s.top_margin = Inches(0.8)
        s.bottom_margin = Inches(0.8)
        s.left_margin = Inches(0.8)
        s.right_margin = Inches(0.8)

    # Configurar estilos nativos Heading 3 y Heading 4 para Google Docs (color negro, Arial)
    from docx.shared import RGBColor
    for h_name, f_size in [('Heading 3', 14), ('Heading 4', 12)]:
        st = doc.styles[h_name]
        st.font.name = 'Arial'
        st.font.size = Pt(f_size)
        st.font.italic = True
        st.font.bold = False
        st.font.color.rgb = RGBColor(0, 0, 0)

    # Tabla de Escenarios (Pág. 12 del PDF)
    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(6)
    p_title.paragraph_format.space_after = Pt(6)
    r_title = p_title.add_run("Definición de escenarios")
    r_title.font.name = 'Arial'
    r_title.font.size = Pt(14)

    add_table(
        doc,
        headers=["Nro. de escenario", "Descripción del escenario"],
        rows=[
            [
                "1",
                "Base de Datos Inicializada con Datos Semilla: El motor MySQL se encuentra activo, la tabla Asociados fue recreada mediante iniciarBD() y poblada con los 20 registros iniciales predefinidos. El manejador Singleton mantiene una conexión JDBC abierta y válida."
            ],
            [
                "2",
                "Servicio de Base de Datos No Disponible: El servidor MySQL se encuentra apagado o con credenciales erróneas, imposibilitando el enlace JDBC con la aplicación."
            ]
        ],
        col_widths=[1.2, 5.3],
        bold_first_col=True
    )

    # 1. guardar(Asociado asociado) (Pág. 14-15 del PDF)
    add_method_block(
        doc,
        signature="Public void guardar(Asociado asociado)",
        escenario_text="(Escenario 1)",
        part_rows=[
            [
                "Objeto de Dominio asociado",
                "Objeto no nulo con atributos consistentes (1)",
                "Referencia nula (2) [null]"
            ],
            [
                "Columna DNI en tabla MySQL",
                "DNI no existente en la base de datos (3)",
                "DNI ya existente en la base de datos (viola UNIQUE) (4)"
            ]
        ],
        bat_rows=[
            [
                "Correcta",
                'dao.guardar(nuevo) con DNI nuevo "99887766"',
                "Se inserta la fila en MySQL; la cantidad total en BD pasa de 20 a 21.",
                "1, 3",
                "testGuardarAsociadoValido()"
            ],
            [
                "Incorrecta",
                'dao.guardar(duplicado) con DNI "12345678" (Juan State)',
                "MySQL rechaza la fila y dispara SQLException (Duplicate entry for key DNI).",
                "1, 4",
                "testGuardarAsociadoDniDuplicadoLanzaSQLException()"
            ],
            [
                "Incorrecta",
                "dao.guardar(null)",
                "Falla en precondición del DAO (assert != null), lanzando AssertionError sin tocar MySQL.",
                "2",
                "testGuardarAsociadoNuloLanzaError()"
            ],
            [
                "Correcta (Mapeo)",
                "dao.guardar(grace) con datos no cruzados",
                "Inserción exitosa; las 7 columnas se recuperan en su correspondencia biunívoca exacta.",
                "1, 3",
                "testMapeoCompletoAlGuardar()"
            ]
        ]
    )

    # 2. cargarConLimite(int limite) (Pág. 15-16 del PDF)
    add_method_block(
        doc,
        signature="Public ArrayList<AsociadoDTO> cargarConLimite(int limite)",
        escenario_text="(Escenario 1)",
        bva_subtitle="(Análisis de Valores Límite: precondición limite > 0)",
        part_rows=[
            [
                "Parámetro limite (int)",
                "Entero mayor a 0:\n\n• Límite inferior válido: limite = 1 (1)\n\n• Valor nominal: limite = 5 (2)",
                "Entero menor o igual a 0:\n\n• Límite frontera inválido: limite = 0 (3)\n\n• Valor inválido exterior: limite = -1 (4)"
            ]
        ],
        bat_rows=[
            [
                "Correcta (Límite)",
                "dao.cargarConLimite(1)",
                "Retorna ArrayList<AsociadoDTO> con tamaño exactamente igual a 1.",
                "1",
                "testCargarConLimiteValido()"
            ],
            [
                "Incorrecta (Límite)",
                "dao.cargarConLimite(0)",
                "Violación de precondición: lanza AssertionError.",
                "3",
                "testCargarConLimiteInvalido()"
            ]
        ]
    )

    # 3. cargar(int id) (Pág. 16-17 del PDF)
    add_method_block(
        doc,
        signature="Public AsociadoDTO cargar(int id)",
        escenario_text="(Escenario 1)",
        bva_subtitle="(Análisis de Valores Límite: precondición id > 0)",
        part_rows=[
            [
                "Parámetro id (int)",
                "Entero mayor a 0 existente en BD:\n\n• Límite inferior válido: id = 1 (1)",
                "Entero menor o igual a 0:\n\n• Límite frontera inválido: id = 0 (2)\n\n• Valor negativo: id = -1 (3)"
            ],
            [
                "Existencia en la tabla",
                "ID registrado en BD (4)",
                "ID no registrado en BD (id > 20) (5)"
            ]
        ],
        bat_rows=[
            [
                "Correcta (Límite)",
                "dao.cargar(1)",
                'Retorna el AsociadoDTO de Juan State (DNI "12345678"). En código original falla con SQLException.',
                "1, 4",
                "testCargarIndividualPorId()"
            ],
            [
                "Incorrecta (Límite)",
                "dao.cargar(0)",
                "Violación de precondición: lanza AssertionError.",
                "2",
                "testCargarPorIdInvalido()"
            ]
        ]
    )

    # 4. actualizar(Asociado asociado) (Pág. 17-18 del PDF)
    add_method_block(
        doc,
        signature="Public void actualizar(Asociado asociado)",
        escenario_text="(Escenario 1)",
        part_rows=[
            [
                "Entidad asociado recibida",
                "Objeto no nulo con DNI existente en BD (1)",
                "Objeto nulo (2)"
            ],
            [
                "Identificador unívoco (DNI)",
                "DNI perteneciente al registro a modificar (3)",
                "DNI inexistente en la tabla (4)"
            ]
        ],
        bat_rows=[
            [
                "Correcta",
                'dao.actualizar(mariaModificada) (DNI "87654321", ID 2)',
                "Modifica la calle y número de María en BD sin afectar otros registros. En código provisto falla por ID fijo.",
                "1, 3",
                "testActualizarAsociadoExistente()"
            ]
        ]
    )

    # 5. eliminar(String dni) (Pág. 18-19 del PDF)
    add_method_block(
        doc,
        signature="Public void eliminar(String dni)",
        escenario_text="(Escenario 1)",
        part_rows=[
            [
                "Parámetro dni (String)",
                "Cadena no nula y no vacía de asociado existente (1)",
                'Cadena vacía "" (2)\n\nReferencia nula null (3)'
            ]
        ],
        bat_rows=[
            [
                "Correcta",
                'dao.eliminar("12345678") (Juan State)',
                "Elimina la fila de Juan State; el total en BD pasa de 20 a 19 registros.",
                "1",
                "testEliminarAsociadoPorDni()"
            ],
            [
                "Incorrecta",
                'dao.eliminar("")',
                "Violación de precondición: lanza AssertionError.",
                "2",
                "testEliminarConDniVacioLanzaError()"
            ],
            [
                "Incorrecta (Bug)",
                "dao.eliminar(null)",
                "Violación de contrato: debe lanzar AssertionError (el código original lo deja filtrar).",
                "3",
                "testEliminarConDniNuloLanzaError()"
            ]
        ]
    )

    doc.save(filename)
    print(f"Saved: {filename}")

if __name__ == "__main__":
    build_docx("temp_tablas_crud_punto2.docx")
