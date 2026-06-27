import pypdf
import sys

def extract_pdf(pdf_path, out_path):
    with open(pdf_path, 'rb') as f:
        reader = pypdf.PdfReader(f)
        text = ''
        for page in reader.pages:
            text += page.extract_text() + '\n'
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(text)

extract_pdf('Consigna detallada.pdf', 'consigna.txt')
extract_pdf('xy1-rulebook-es (1).pdf', 'rulebook.txt')
