# Configuration file for the Sphinx documentation builder.
# See https://www.sphinx-doc.org/en/master/usage/configuration.html

project = 'ADAPT'
copyright = '2015-2026, David Barry'
author = 'David Barry'
release = '4.0.24'

extensions = [
    'myst_parser',
]

templates_path = ['_templates']
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']

html_theme = 'sphinx_rtd_theme'
html_static_path = ['_static']

# MyST treats every Markdown file in the tree as a document.
source_suffix = {
    '.rst': 'restructuredtext',
    '.md': 'markdown',
}

myst_heading_anchors = 3
myst_enable_extensions = [
    'colon_fence',
    'deflist',
]
