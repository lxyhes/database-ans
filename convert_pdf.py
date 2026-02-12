from pdf2docx import Converter

pdf_file = r'e:\data-analysis-assistant\软件开发工程师-王子瑞.pdf'
docx_file = r'e:\data-analysis-assistant\软件开发工程师-王子瑞.docx'

cv = Converter(pdf_file)
cv.convert(
    docx_file, 
    start=0, 
    end=None,
    multi_processing=False,
    debug=False,
    keep_format=True,
    min_vertical_gap_width=5,
    min_horizontal_gap_width=5
)
cv.close()
print('转换完成!')
