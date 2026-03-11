import { PageIntro } from "../../components/common/Ui";

export function AdminStatsPage() {
  return (
    <main className="page-shell">
      <section className="surface-card">
        <PageIntro
          eyebrow="Admin"
          title="통계"
          description="주간 및 월간 통계 데이터를 표시합니다."
        />
      </section>
    </main>
  );
}
