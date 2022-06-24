const siteConfig = {
    title: "Scala QL",
    tagline: "Statically typed query DSL for Scala.",
    url: "https://scala-ql.vhonta.dev",
    baseUrl: "/",
    projectName: 'scala-ql',
    favicon: 'img/favicon/favicon.ico',
    presets: [
        [
            '@docusaurus/preset-classic',
            {
            docs: {
                path: '../docs/target/mdoc',
                sidebarPath: require.resolve('./sidebars.json'),
            },
            // theme: {
            //   customCss: [require.resolve('./src/css/custom.css')],
            // },
            },
        ],
    ],
    themeConfig: {
      prism: {
        theme: require('prism-react-renderer/themes/nightOwl'),
        additionalLanguages: [
          'java',
          'scala',
        ],
      },
      announcementBar: {
        id: 'support_ukraine',
        content:
          'Support Ukraine 🇺🇦 <a target="_blank" rel="noopener noreferrer" \
          href="http://u24.gov.ua/"> Help Provide Aid to Ukraine</a>.',
        backgroundColor: '#20232a',
        textColor: '#fff',
        isCloseable: false,
      },
      navbar: {
        title: 'Scala QL',
        // logo: {
        //   alt: 'Create React App Logo',
        //   src: 'img/logo.svg',
        // },
        items: [
          { to: 'docs/getting-started', label: 'Docs', position: 'right' },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Get Started',
                to: 'docs/getting-started',
              },
              {
                label: 'Queries',
                href: 'docs/queries',
              },
            ],
          },
        ],
        // logo: {
        //   alt: 'Facebook Open Source Logo',
        //   src: 'img/oss_logo.png',
        // },
        copyright: `Copyright © ${new Date().getFullYear()} Vitalii Honta`,
      }
    }
};
  
module.exports = siteConfig;