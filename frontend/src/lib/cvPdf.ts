import { jsPDF } from 'jspdf';

export interface ManualCvInput {
  fullName: string;
  headline: string;
  email: string;
  phone: string;
  location: string;
  links: string;
  summary: string;
  experiences: string[];
  educations: string[];
  skills: string[];
}

export interface GeneratedPdf {
  blob: Blob;
  base64: string;
  filename: string;
}

const ACCENT: [number, number, number] = [37, 99, 235];
const DARK: [number, number, number] = [17, 24, 39];
const GRAY: [number, number, number] = [107, 114, 128];
const LINE: [number, number, number] = [209, 213, 219];

const PAGE_W = 595.28;
const PAGE_H = 841.89;
const MARGIN = 48;
const CONTENT_W = PAGE_W - MARGIN * 2;

const slugify = (input: string): string => {
  const slug = input
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/(^-|-$)/g, '');
  return slug.length === 0 ? 'cv' : slug;
};

export const generateCvPdf = (data: ManualCvInput): GeneratedPdf => {
  const doc = new jsPDF({ unit: 'pt', format: 'a4' });
  let y = MARGIN;

  const ensureSpace = (needed: number) => {
    if (y + needed > PAGE_H - MARGIN) {
      doc.addPage();
      y = MARGIN;
    }
  };

  const paragraph = (
    text: string,
    size: number,
    color: [number, number, number],
    fontStyle: 'normal' | 'bold' | 'italic',
    lineHeight: number,
  ) => {
    doc.setFont('helvetica', fontStyle);
    doc.setFontSize(size);
    doc.setTextColor(color[0], color[1], color[2]);
    const lines = doc.splitTextToSize(text, CONTENT_W) as string[];
    lines.forEach((line) => {
      ensureSpace(lineHeight);
      doc.text(line, MARGIN, y);
      y += lineHeight;
    });
  };

  const sectionTitle = (label: string) => {
    y += 10;
    ensureSpace(26);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(ACCENT[0], ACCENT[1], ACCENT[2]);
    doc.text(label.toUpperCase(), MARGIN, y);
    y += 6;
    doc.setDrawColor(LINE[0], LINE[1], LINE[2]);
    doc.setLineWidth(0.8);
    doc.line(MARGIN, y, PAGE_W - MARGIN, y);
    y += 14;
  };

  doc.setFont('helvetica', 'bold');
  doc.setFontSize(24);
  doc.setTextColor(DARK[0], DARK[1], DARK[2]);
  doc.text(data.fullName.trim(), MARGIN, y);
  y += 26;

  if (data.headline.trim().length > 0) {
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(12);
    doc.setTextColor(ACCENT[0], ACCENT[1], ACCENT[2]);
    doc.text(data.headline.trim(), MARGIN, y);
    y += 18;
  }

  const contactParts = [data.email, data.phone, data.location, data.links]
    .map((p) => p.trim())
    .filter((p) => p.length > 0);
  if (contactParts.length > 0) {
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9.5);
    doc.setTextColor(GRAY[0], GRAY[1], GRAY[2]);
    const contactLines = doc.splitTextToSize(contactParts.join('  |  '), CONTENT_W) as string[];
    contactLines.forEach((line) => {
      doc.text(line, MARGIN, y);
      y += 13;
    });
  }

  y += 6;
  doc.setDrawColor(ACCENT[0], ACCENT[1], ACCENT[2]);
  doc.setLineWidth(1.4);
  doc.line(MARGIN, y, PAGE_W - MARGIN, y);
  y += 6;

  if (data.summary.trim().length > 0) {
    sectionTitle('Profile');
    paragraph(data.summary.trim(), 10, DARK, 'normal', 14);
  }

  const entries = (label: string, items: string[]) => {
    const cleaned = items.map((i) => i.trim()).filter((i) => i.length > 0);
    if (cleaned.length === 0) return;
    sectionTitle(label);
    cleaned.forEach((item) => {
      paragraph(item, 10, DARK, 'normal', 14);
      y += 4;
    });
  };

  entries('Experience', data.experiences);
  entries('Education', data.educations);

  const skills = data.skills.map((s) => s.trim()).filter((s) => s.length > 0);
  if (skills.length > 0) {
    sectionTitle('Skills');
    paragraph(skills.join('  •  '), 10, DARK, 'normal', 14);
  }

  const blob = doc.output('blob');
  const bytes = new Uint8Array(doc.output('arraybuffer') as ArrayBuffer);
  let binary = '';
  for (let i = 0; i < bytes.length; i += 1) {
    binary += String.fromCharCode(bytes[i]);
  }
  const base64 = btoa(binary);
  const filename = `${slugify(data.fullName || data.headline || 'cv')}.pdf`;

  return { blob, base64, filename };
};
