import re
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, fill_color):
    tcPr = cell._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_color}"/>'))

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def create_styled_docx(md_path, docx_path):
    doc = Document()
    
    # Configure page margins
    sections = doc.sections
    for section in sections:
        section.top_margin = Inches(0.8)
        section.bottom_margin = Inches(0.8)
        section.left_margin = Inches(0.8)
        section.right_margin = Inches(0.8)

    # Base styling
    normal_style = doc.styles['Normal']
    normal_font = normal_style.font
    normal_font.name = 'Calibri'
    normal_font.size = Pt(11)
    normal_font.color.rgb = RGBColor(40, 40, 40)

    with open(md_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    i = 0
    while i < len(lines):
        line = lines[i].strip()
        
        if not line or line == '---':
            i += 1
            continue

        # Header 1
        if line.startswith('# '):
            text = line[2:].strip()
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(16)
            p.paragraph_format.space_after = Pt(6)
            run = p.add_run(text)
            run.font.size = Pt(20)
            run.font.bold = True
            run.font.color.rgb = RGBColor(26, 54, 93) # Deep Blue
            i += 1
            continue

        # Header 2
        if line.startswith('## '):
            text = line[3:].strip()
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(14)
            p.paragraph_format.space_after = Pt(4)
            run = p.add_run(text)
            run.font.size = Pt(15)
            run.font.bold = True
            run.font.color.rgb = RGBColor(43, 84, 126)
            i += 1
            continue

        # Header 3
        if line.startswith('### '):
            text = line[4:].strip()
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(10)
            p.paragraph_format.space_after = Pt(2)
            run = p.add_run(text)
            run.font.size = Pt(12.5)
            run.font.bold = True
            run.font.color.rgb = RGBColor(45, 55, 72)
            i += 1
            continue

        # Header 4
        if line.startswith('#### '):
            text = line[5:].strip()
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(8)
            p.paragraph_format.space_after = Pt(2)
            run = p.add_run(text)
            run.font.size = Pt(11.5)
            run.font.bold = True
            run.font.color.rgb = RGBColor(74, 85, 104)
            i += 1
            continue

        # Table detection
        if line.startswith('|') and line.endswith('|'):
            table_lines = []
            while i < len(lines) and lines[i].strip().startswith('|') and lines[i].strip().endswith('|'):
                table_lines.append(lines[i].strip())
                i += 1
            
            # Parse table
            rows_data = []
            for tl in table_lines:
                # check if separator
                if re.match(r'^\|(\s*:?-+:?\s*\|)+$', tl):
                    continue
                cells = [c.strip() for c in tl.strip('|').split('|')]
                rows_data.append(cells)
            
            if rows_data:
                num_cols = len(rows_data[0])
                table = doc.add_table(rows=len(rows_data), cols=num_cols)
                table.alignment = WD_TABLE_ALIGNMENT.CENTER
                table.autofit = True
                
                # Format table
                for r_idx, row in enumerate(rows_data):
                    is_header = (r_idx == 0)
                    for c_idx, cell_value in enumerate(row):
                        if c_idx < len(table.rows[r_idx].cells):
                            cell = table.cell(r_idx, c_idx)
                            set_cell_margins(cell, 80, 80, 120, 120)
                            cell_p = cell.paragraphs[0]
                            cell_p.paragraph_format.space_before = Pt(2)
                            cell_p.paragraph_format.space_after = Pt(2)
                            
                            # Clean markdown bold/links
                            clean_text = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', cell_value)
                            
                            # Header styling
                            if is_header:
                                set_cell_background(cell, "2B547E")
                                run = cell_p.add_run(clean_text)
                                run.font.bold = True
                                run.font.size = Pt(9.5)
                                run.font.color.rgb = RGBColor(255, 255, 255)
                            else:
                                if r_idx % 2 == 1:
                                    set_cell_background(cell, "F7FAFC")
                                else:
                                    set_cell_background(cell, "EDF2F7")
                                
                                # Add formatted runs
                                parts = re.split(r'(\*\*[^*]+\*\*)', clean_text)
                                for part in parts:
                                    if part.startswith('**') and part.endswith('**'):
                                        r = cell_p.add_run(part[2:-2])
                                        r.font.bold = True
                                    else:
                                        r = cell_p.add_run(part)
                                    r.font.size = Pt(9)
                                    r.font.color.rgb = RGBColor(45, 55, 72)
                
                # Add spacing after table
                after_p = doc.add_paragraph()
                after_p.paragraph_format.space_before = Pt(4)
                after_p.paragraph_format.space_after = Pt(4)
            continue

        # Bullet list item
        if line.startswith('* ') or line.startswith('- '):
            text = line[2:].strip()
            p = doc.add_paragraph(style='List Bullet')
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            format_paragraph_text(p, text)
            i += 1
            continue

        # Numbered list item
        num_match = re.match(r'^(\d+)\.\s+(.*)$', line)
        if num_match:
            text = num_match.group(2).strip()
            p = doc.add_paragraph(style='List Number')
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            format_paragraph_text(p, text)
            i += 1
            continue

        # Regular paragraph
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(3)
        p.paragraph_format.space_after = Pt(4)
        format_paragraph_text(p, line)
        i += 1

    doc.save(docx_path)
    print(f"Document saved successfully: {docx_path}")

def format_paragraph_text(p, text):
    # Replace markdown links with text: [Link text](url) -> Link text
    text = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', text)
    # Parse bold formatting
    parts = re.split(r'(\*\*[^*]+\*\*)', text)
    for part in parts:
        if part.startswith('**') and part.endswith('**'):
            run = p.add_run(part[2:-2])
            run.font.bold = True
        else:
            p.add_run(part)

if __name__ == '__main__':
    create_styled_docx('INFORME_ESTRATEGIA_DE_TESTING.md', 'INFORME_ESTRATEGIA_DE_TESTING.docx')
