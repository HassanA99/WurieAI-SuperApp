from pathlib import Path
from xml.sax.saxutils import escape

from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import Paragraph, Preformatted, SimpleDocTemplate, Spacer

source_path = Path(__file__).with_name("secrets-and-deployment-guide.md")
output_path = Path(__file__).with_name("WurieAI-secrets-and-deployment-guide.pdf")
source = source_path.read_text(encoding="utf-8")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(
    name="WurieTitle", parent=styles["Title"], fontSize=22, leading=27,
    spaceAfter=16, textColor="#34205f",
))
styles.add(ParagraphStyle(
    name="WurieHeading", parent=styles["Heading1"], fontSize=15, leading=19,
    spaceBefore=12, spaceAfter=7, textColor="#34205f",
))
styles.add(ParagraphStyle(
    name="WurieBody", parent=styles["BodyText"], fontSize=9.5, leading=13,
    spaceAfter=5,
))
styles.add(ParagraphStyle(
    name="WurieBullet", parent=styles["BodyText"], fontSize=9.5, leading=13,
    leftIndent=15, firstLineIndent=-8, spaceAfter=3,
))
styles.add(ParagraphStyle(
    name="WurieCode", parent=styles["Code"], fontName="Courier", fontSize=8,
    leading=10, leftIndent=14, spaceBefore=3, spaceAfter=6, backColor="#f1eff6",
))

story = []
in_code = False
code_lines = []
for line in source.splitlines():
    if line.startswith("```"):
        if in_code:
            story.append(Preformatted("\n".join(code_lines), styles["WurieCode"]))
            code_lines = []
        in_code = not in_code
        continue
    if in_code:
        code_lines.append(line)
        continue
    if not line:
        story.append(Spacer(1, 4))
        continue
    if line.startswith("# "):
        story.append(Paragraph(escape(line[2:]), styles["WurieTitle"]))
    elif line.startswith("## "):
        story.append(Paragraph(escape(line[3:]), styles["WurieHeading"]))
    elif line.startswith("- "):
        story.append(Paragraph("&bull; " + escape(line[2:]), styles["WurieBullet"]))
    else:
        story.append(Paragraph(escape(line), styles["WurieBody"]))


def add_footer(canvas, document):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColorRGB(0.4, 0.4, 0.4)
    canvas.drawString(0.7 * inch, 0.45 * inch, "WurieAI API and Secret Setup Guide")
    canvas.drawRightString(7.8 * inch, 0.45 * inch, "Page %d" % document.page)
    canvas.restoreState()


document = SimpleDocTemplate(
    str(output_path),
    pagesize=letter,
    rightMargin=0.7 * inch,
    leftMargin=0.7 * inch,
    topMargin=0.65 * inch,
    bottomMargin=0.7 * inch,
    title="WurieAI API and Secret Setup Guide",
)
document.build(story, onFirstPage=add_footer, onLaterPages=add_footer)
print(output_path)
