import React from 'react';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import useBaseUrl from '@docusaurus/useBaseUrl';
import Layout from '@theme/Layout';
import CodeBlock from '@theme/CodeBlock';
import clsx from 'clsx';
import styles from './styles.module.css';

const features = [
  {
    title: 'Simple',
    content: [
      "Querying data is simple and straightforward.",
      "It's not required to know advanced functional typeclasses to use the lirary."
    ]
  },
  {
    title: 'Seamless',
    content: [
      "Read and write to various datasources seamlessly.",
      "Scala QL supports CSV, JSON, Excel, HTML and more!"
    ]
  },
  {
    title: 'Static types',
    content: [
      "Benefit from having statically typed queries!",
      "Scala QL allows to work with plain-old case classes, which makes your code type-checked"
    ]
  },
];

const exampleCode = `import scalaql._
import java.nio.charset.StandardCharsets
import java.nio.file.Paths

val query: Query[From[Student] with From[Faculty], FacultyStats] =
  select[Student]
    .where(_.age >= 18)
    .join(select[Faculty])
    .on(_.faculty == _.name)
    .groupBy { case (_, faculty) => faculty.name }
    .aggregate((_, studentsWithFaculties) => studentsWithFaculties.avgBy { case (student, _) => student.grade })
    .map((FacultyStats.apply _).tupled)

val studentsPath = Paths.get("students.csv")
val facultiesPath = Paths.get("faculties.json")
val outPath = Paths.get("faculty_stats.csv")

query
  .foreach(
    csv.write.file[FacultyStats](outPath)
  )
  .run(
    from(
      csv.read.file[Student](studentsPath)
    ) & from(
      json.read.file[Faculty](facultiesPath)
    )
  )
`

export default function Home() {
  const context = useDocusaurusContext();
  const { siteConfig = {} } = context;

  return (
    <Layout
      permalink={'/'}
      description={'Simple statically typed query DSL for scala'}
    >
      <div className={clsx('hero hero--dark', styles.heroBanner)}>
        <div className="container">
          {/* <img
             className={clsx(styles.heroBannerLogo, 'margin-vert--md')}
             alt="Create React App logo"
             src={useBaseUrl('img/logo.svg')}
           /> */}
          <h1 className="hero__title">{siteConfig.title}</h1>
          <p className="hero__subtitle">{siteConfig.tagline}</p>

          <div className={styles.getStarted}>
            <Link
              className="button button--outline button--primary button--lg"
              to={useBaseUrl('docs/getting-started')}
            >
              Get Started
            </Link>
          </div>
        </div>
      </div>
      {features && features.length && (
        <div className={styles.features}>
          <div className="container">
            <div className="row">
              {features.map(({ title, content }, idx) => (
                <div key={idx} className={clsx('col col--4', styles.feature)}>
                  <h2>{title}</h2>
                  {content.map(line => (<p>{line}</p>))}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
      <div className={styles.gettingStartedSection}>
        <div className="container padding-vert--xl text--left">
          <div className="row">
            <div className="col col--6 col--offset-1">
              <h2>Get started easily</h2>
              <p>This is an example how to query data from various sources easily.</p>
              <br />
              <CodeBlock className={clsx("language-scala", styles.exampleCodeBlock)}>
                {exampleCode}
              </CodeBlock>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}