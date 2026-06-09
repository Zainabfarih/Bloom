import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { X, FileText, Download } from 'lucide-react';
import { cvApi } from '../../api/cv.api';
import { useToast } from '../ui/Toast';
import { Spinner } from '../ui/Spinner';
import { generateCvPdf } from '../../lib/cvPdf';
import { saveBlob } from '../../lib/download';
import type { CvResponse } from '../../types';
import styles from './ManualCvForm.module.css';

interface Props {
  onClose: () => void;
}

const toLines = (value: string): string[] =>
  value
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l.length > 0);

const toSkills = (value: string): string[] =>
  value
    .split(/[\n,]/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0);

export const ManualCvForm = ({ onClose }: Props) => {
  const qc = useQueryClient();
  const toast = useToast();

  const [fullName, setFullName] = useState('');
  const [headline, setHeadline] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [location, setLocation] = useState('');
  const [links, setLinks] = useState('');
  const [summary, setSummary] = useState('');
  const [experiences, setExperiences] = useState('');
  const [educations, setEducations] = useState('');
  const [skills, setSkills] = useState('');
  const [error, setError] = useState('');

  const buildInput = () => ({
    fullName: fullName.trim(),
    headline: headline.trim(),
    email: email.trim(),
    phone: phone.trim(),
    location: location.trim(),
    links: links.trim(),
    summary: summary.trim(),
    experiences: toLines(experiences),
    educations: toLines(educations),
    skills: toSkills(skills),
  });

  const validate = (): string | null => {
    if (fullName.trim().length === 0) return 'Full name is required.';
    if (summary.trim().length === 0) return 'Summary is required.';
    if (toLines(experiences).length === 0) return 'Add at least one experience.';
    if (toLines(educations).length === 0) return 'Add at least one education entry.';
    if (toSkills(skills).length === 0) return 'Add at least one skill.';
    return null;
  };

  const createMutation = useMutation({
    mutationFn: (): Promise<CvResponse> => {
      const input = buildInput();
      const pdf = generateCvPdf(input);
      const title = input.headline
        ? `${input.fullName} — ${input.headline}`
        : input.fullName;
      return cvApi.createManualCv({
        title: title.slice(0, 255),
        summary: input.summary,
        experiences: input.experiences,
        educations: input.educations,
        skills: input.skills,
        pdfBase64: pdf.base64,
      });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cvs'] });
      toast.success('CV created and saved');
      onClose();
    },
    onError: (err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } };
      const msg = e?.response?.data?.message ?? 'Could not create the CV. Please try again.';
      setError(msg);
      toast.error(msg);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError('');
    createMutation.mutate();
  };

  const handlePreview = () => {
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError('');
    const pdf = generateCvPdf(buildInput());
    saveBlob(pdf.blob, pdf.filename);
  };

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true" aria-label="Create CV manually">
      <div className={styles.modal}>
        <div className={styles.header}>
          <h3 className={styles.title}><FileText size={16} /> Create a CV manually</h3>
          <button className="btn btn--ghost btn--sm" onClick={onClose} aria-label="Close">
            <X size={16} />
          </button>
        </div>

        <form className={styles.body} onSubmit={handleSubmit}>
          <div className={styles.grid}>
            <label className={styles.field}>
              <span>Full name *</span>
              <input value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Jane Doe" />
            </label>
            <label className={styles.field}>
              <span>Headline</span>
              <input value={headline} onChange={(e) => setHeadline(e.target.value)} placeholder="Software Engineer" />
            </label>
            <label className={styles.field}>
              <span>Email</span>
              <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="jane@email.com" />
            </label>
            <label className={styles.field}>
              <span>Phone</span>
              <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+212 600 000000" />
            </label>
            <label className={styles.field}>
              <span>Location</span>
              <input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="Rabat, Morocco" />
            </label>
            <label className={styles.field}>
              <span>Links</span>
              <input value={links} onChange={(e) => setLinks(e.target.value)} placeholder="github.com/jane" />
            </label>
          </div>

          <label className={styles.field}>
            <span>Summary *</span>
            <textarea value={summary} onChange={(e) => setSummary(e.target.value)} rows={3}
              placeholder="A short professional summary." />
          </label>

          <label className={styles.field}>
            <span>Experience * (one per line)</span>
            <textarea value={experiences} onChange={(e) => setExperiences(e.target.value)} rows={4}
              placeholder={'Software Engineer at Acme (2022-2024) — built X, Y, Z\nIntern at Beta (2021) — did A, B'} />
          </label>

          <label className={styles.field}>
            <span>Education * (one per line)</span>
            <textarea value={educations} onChange={(e) => setEducations(e.target.value)} rows={3}
              placeholder={'MSc Computer Science, University X (2024)\nBSc Software Engineering, University Y (2022)'} />
          </label>

          <label className={styles.field}>
            <span>Skills * (comma or line separated)</span>
            <textarea value={skills} onChange={(e) => setSkills(e.target.value)} rows={2}
              placeholder="Java, Spring Boot, React, PostgreSQL, Docker" />
          </label>

          {error && <p className={styles.error} role="alert">{error}</p>}

          <div className={styles.actions}>
            <button type="button" className="btn btn--ghost" onClick={handlePreview}>
              <Download size={15} /> Preview PDF
            </button>
            <button type="submit" className="btn btn--primary" disabled={createMutation.isPending}>
              {createMutation.isPending ? <Spinner size={15} /> : 'Generate & Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
