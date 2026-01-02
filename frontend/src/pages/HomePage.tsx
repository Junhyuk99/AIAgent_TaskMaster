import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function HomePage() {
  const { t } = useTranslation();

  return (
    <div className="min-h-screen bg-white dark:bg-gray-900">
      {/* Navigation */}
      <nav className="fixed top-0 left-0 right-0 z-50 bg-white/80 dark:bg-gray-900/80 backdrop-blur-md border-b border-gray-200 dark:border-gray-800">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-purple-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-lg">A</span>
              </div>
              <span className="text-xl font-bold text-gray-900 dark:text-white">AI Agent</span>
            </div>
            <div className="flex items-center space-x-4">
              <Link
                to="/login"
                className="text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white transition-colors"
              >
                {t('auth.login', 'Login')}
              </Link>
              <Link
                to="/register"
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
              >
                {t('auth.register', 'Get Started')}
              </Link>
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-4 sm:px-6 lg:px-8 bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 dark:from-gray-900 dark:via-gray-900 dark:to-gray-800">
        <div className="container mx-auto max-w-6xl">
          <div className="text-center">
            <div className="inline-flex items-center px-4 py-2 bg-primary-100 dark:bg-primary-900/30 rounded-full text-primary-700 dark:text-primary-300 text-sm font-medium mb-6">
              <span className="w-2 h-2 bg-green-500 rounded-full mr-2 animate-pulse"></span>
              {t('landing.badge', 'Now Available')}
            </div>
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-gray-900 dark:text-white mb-6 leading-tight">
              {t('landing.heroTitle', 'Build Intelligent AI Agents')}
              <br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-600 to-purple-600">
                {t('landing.heroHighlight', 'Without Writing Code')}
              </span>
            </h1>
            <p className="text-lg sm:text-xl text-gray-600 dark:text-gray-300 mb-10 max-w-3xl mx-auto leading-relaxed">
              {t('landing.heroDescription', 'Create, customize, and deploy AI agents with custom functions and knowledge bases. Integrate with any LLM provider and build powerful conversational experiences.')}
            </p>
            <div className="flex flex-col sm:flex-row justify-center gap-4">
              <Link
                to="/register"
                className="px-8 py-4 bg-primary-600 text-white rounded-xl hover:bg-primary-700 transition-all font-semibold text-lg shadow-lg shadow-primary-500/25 hover:shadow-xl hover:shadow-primary-500/30 hover:-translate-y-0.5"
              >
                {t('landing.startFree', 'Start Free')}
              </Link>
              <Link
                to="/login"
                className="px-8 py-4 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700 transition-all font-semibold text-lg border border-gray-200 dark:border-gray-700"
              >
                {t('landing.viewDemo', 'View Demo')}
              </Link>
            </div>
          </div>

          {/* Hero Image/Illustration */}
          <div className="mt-16 relative">
            <div className="absolute inset-0 bg-gradient-to-t from-blue-50 dark:from-gray-900 to-transparent z-10 pointer-events-none"></div>
            <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl border border-gray-200 dark:border-gray-700 overflow-hidden">
              <div className="bg-gray-100 dark:bg-gray-700 px-4 py-3 flex items-center space-x-2">
                <div className="w-3 h-3 rounded-full bg-red-500"></div>
                <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
                <div className="w-3 h-3 rounded-full bg-green-500"></div>
              </div>
              <div className="p-6 bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-800 dark:to-gray-900 min-h-[300px] flex items-center justify-center">
                <div className="grid grid-cols-3 gap-6 w-full max-w-2xl">
                  <div className="bg-white dark:bg-gray-700 p-4 rounded-xl shadow-sm">
                    <div className="w-10 h-10 bg-blue-100 dark:bg-blue-900 rounded-lg flex items-center justify-center mb-3">
                      <span className="text-xl">🤖</span>
                    </div>
                    <div className="h-2 bg-gray-200 dark:bg-gray-600 rounded w-3/4 mb-2"></div>
                    <div className="h-2 bg-gray-100 dark:bg-gray-500 rounded w-1/2"></div>
                  </div>
                  <div className="bg-white dark:bg-gray-700 p-4 rounded-xl shadow-sm">
                    <div className="w-10 h-10 bg-purple-100 dark:bg-purple-900 rounded-lg flex items-center justify-center mb-3">
                      <span className="text-xl">⚡</span>
                    </div>
                    <div className="h-2 bg-gray-200 dark:bg-gray-600 rounded w-3/4 mb-2"></div>
                    <div className="h-2 bg-gray-100 dark:bg-gray-500 rounded w-1/2"></div>
                  </div>
                  <div className="bg-white dark:bg-gray-700 p-4 rounded-xl shadow-sm">
                    <div className="w-10 h-10 bg-green-100 dark:bg-green-900 rounded-lg flex items-center justify-center mb-3">
                      <span className="text-xl">📚</span>
                    </div>
                    <div className="h-2 bg-gray-200 dark:bg-gray-600 rounded w-3/4 mb-2"></div>
                    <div className="h-2 bg-gray-100 dark:bg-gray-500 rounded w-1/2"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8 bg-white dark:bg-gray-900">
        <div className="container mx-auto max-w-6xl">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white mb-4">
              {t('landing.featuresTitle', 'Everything You Need')}
            </h2>
            <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
              {t('landing.featuresDescription', 'Build powerful AI agents with our comprehensive platform')}
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {/* Feature 1 */}
            <div className="group p-6 bg-gray-50 dark:bg-gray-800 rounded-2xl hover:bg-white dark:hover:bg-gray-700 hover:shadow-xl transition-all duration-300">
              <div className="w-14 h-14 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                <svg className="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-3">
                {t('landing.feature1Title', 'Custom AI Agents')}
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                {t('landing.feature1Desc', 'Create agents with custom personalities, system prompts, and behaviors tailored to your needs.')}
              </p>
            </div>

            {/* Feature 2 */}
            <div className="group p-6 bg-gray-50 dark:bg-gray-800 rounded-2xl hover:bg-white dark:hover:bg-gray-700 hover:shadow-xl transition-all duration-300">
              <div className="w-14 h-14 bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                <svg className="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-3">
                {t('landing.feature2Title', 'Function Calling')}
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                {t('landing.feature2Desc', 'Extend agent capabilities with custom functions, API integrations, and code execution.')}
              </p>
            </div>

            {/* Feature 3 */}
            <div className="group p-6 bg-gray-50 dark:bg-gray-800 rounded-2xl hover:bg-white dark:hover:bg-gray-700 hover:shadow-xl transition-all duration-300">
              <div className="w-14 h-14 bg-gradient-to-br from-green-500 to-green-600 rounded-xl flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                <svg className="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-3">
                {t('landing.feature3Title', 'Knowledge Bases')}
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                {t('landing.feature3Desc', 'Upload documents and create searchable knowledge bases for context-aware responses.')}
              </p>
            </div>

            {/* Feature 4 */}
            <div className="group p-6 bg-gray-50 dark:bg-gray-800 rounded-2xl hover:bg-white dark:hover:bg-gray-700 hover:shadow-xl transition-all duration-300">
              <div className="w-14 h-14 bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                <svg className="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-3">
                {t('landing.feature4Title', 'Multi-LLM Support')}
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                {t('landing.feature4Desc', 'Connect to OpenAI, Anthropic, Google, Ollama, and other LLM providers.')}
              </p>
            </div>

            {/* Feature 5 */}
            <div className="group p-6 bg-gray-50 dark:bg-gray-800 rounded-2xl hover:bg-white dark:hover:bg-gray-700 hover:shadow-xl transition-all duration-300">
              <div className="w-14 h-14 bg-gradient-to-br from-pink-500 to-pink-600 rounded-xl flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                <svg className="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-3">
                {t('landing.feature5Title', 'Embeddable Widget')}
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                {t('landing.feature5Desc', 'Embed your AI agents on any website with a simple code snippet.')}
              </p>
            </div>

            {/* Feature 6 */}
            <div className="group p-6 bg-gray-50 dark:bg-gray-800 rounded-2xl hover:bg-white dark:hover:bg-gray-700 hover:shadow-xl transition-all duration-300">
              <div className="w-14 h-14 bg-gradient-to-br from-cyan-500 to-cyan-600 rounded-xl flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                <svg className="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                </svg>
              </div>
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-3">
                {t('landing.feature6Title', 'Usage Analytics')}
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                {t('landing.feature6Desc', 'Track conversations, token usage, and agent performance with detailed analytics.')}
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Use Cases Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8 bg-gray-50 dark:bg-gray-800">
        <div className="container mx-auto max-w-6xl">
          <div className="text-center mb-16">
            <h2 className="text-3xl sm:text-4xl font-bold text-gray-900 dark:text-white mb-4">
              {t('landing.useCasesTitle', 'Use Cases')}
            </h2>
            <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
              {t('landing.useCasesDescription', 'See how teams are using AI agents to transform their workflows')}
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="bg-white dark:bg-gray-900 p-8 rounded-2xl shadow-sm hover:shadow-lg transition-shadow">
              <div className="flex items-center mb-4">
                <div className="w-12 h-12 bg-blue-100 dark:bg-blue-900 rounded-xl flex items-center justify-center mr-4">
                  <span className="text-2xl">💬</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
                  {t('landing.useCase1Title', 'Customer Support')}
                </h3>
              </div>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                {t('landing.useCase1Desc', 'Automate customer inquiries with AI agents that understand your products and policies.')}
              </p>
              <ul className="space-y-2 text-sm text-gray-500 dark:text-gray-400">
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 24/7 availability</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Instant responses</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Consistent quality</li>
              </ul>
            </div>

            <div className="bg-white dark:bg-gray-900 p-8 rounded-2xl shadow-sm hover:shadow-lg transition-shadow">
              <div className="flex items-center mb-4">
                <div className="w-12 h-12 bg-purple-100 dark:bg-purple-900 rounded-xl flex items-center justify-center mr-4">
                  <span className="text-2xl">📊</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
                  {t('landing.useCase2Title', 'Data Analysis')}
                </h3>
              </div>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                {t('landing.useCase2Desc', 'Query your data with natural language and get instant insights.')}
              </p>
              <ul className="space-y-2 text-sm text-gray-500 dark:text-gray-400">
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Natural language queries</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Custom visualizations</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Automated reports</li>
              </ul>
            </div>

            <div className="bg-white dark:bg-gray-900 p-8 rounded-2xl shadow-sm hover:shadow-lg transition-shadow">
              <div className="flex items-center mb-4">
                <div className="w-12 h-12 bg-green-100 dark:bg-green-900 rounded-xl flex items-center justify-center mr-4">
                  <span className="text-2xl">🎓</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
                  {t('landing.useCase3Title', 'Education & Training')}
                </h3>
              </div>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                {t('landing.useCase3Desc', 'Create personalized learning experiences with AI tutors and assistants.')}
              </p>
              <ul className="space-y-2 text-sm text-gray-500 dark:text-gray-400">
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Personalized learning</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Interactive Q&A</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Progress tracking</li>
              </ul>
            </div>

            <div className="bg-white dark:bg-gray-900 p-8 rounded-2xl shadow-sm hover:shadow-lg transition-shadow">
              <div className="flex items-center mb-4">
                <div className="w-12 h-12 bg-orange-100 dark:bg-orange-900 rounded-xl flex items-center justify-center mr-4">
                  <span className="text-2xl">🔧</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
                  {t('landing.useCase4Title', 'Developer Tools')}
                </h3>
              </div>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                {t('landing.useCase4Desc', 'Build coding assistants that understand your codebase and documentation.')}
              </p>
              <ul className="space-y-2 text-sm text-gray-500 dark:text-gray-400">
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Code explanations</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Documentation search</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> Bug fixing help</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8 bg-gradient-to-r from-primary-600 to-purple-600">
        <div className="container mx-auto max-w-4xl text-center">
          <h2 className="text-3xl sm:text-4xl font-bold text-white mb-6">
            {t('landing.ctaTitle', 'Ready to Build Your AI Agent?')}
          </h2>
          <p className="text-lg text-white/80 mb-10 max-w-2xl mx-auto">
            {t('landing.ctaDescription', 'Join thousands of developers and businesses building intelligent AI experiences.')}
          </p>
          <div className="flex flex-col sm:flex-row justify-center gap-4">
            <Link
              to="/register"
              className="px-8 py-4 bg-white text-primary-600 rounded-xl hover:bg-gray-100 transition-all font-semibold text-lg shadow-lg"
            >
              {t('landing.ctaButton', 'Get Started Free')}
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 px-4 sm:px-6 lg:px-8 bg-gray-900 text-gray-400">
        <div className="container mx-auto max-w-6xl">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">
            <div>
              <div className="flex items-center space-x-2 mb-4">
                <div className="w-8 h-8 bg-gradient-to-br from-primary-500 to-purple-600 rounded-lg flex items-center justify-center">
                  <span className="text-white font-bold text-lg">A</span>
                </div>
                <span className="text-xl font-bold text-white">AI Agent</span>
              </div>
              <p className="text-sm">
                {t('landing.footerTagline', 'Build intelligent AI agents for your business.')}
              </p>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">{t('landing.footerProduct', 'Product')}</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white transition-colors">{t('landing.footerFeatures', 'Features')}</a></li>
                <li><a href="#" className="hover:text-white transition-colors">{t('landing.footerPricing', 'Pricing')}</a></li>
                <li><a href="#" className="hover:text-white transition-colors">{t('landing.footerDocs', 'Documentation')}</a></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">{t('landing.footerCompany', 'Company')}</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white transition-colors">{t('landing.footerAbout', 'About')}</a></li>
                <li><a href="#" className="hover:text-white transition-colors">{t('landing.footerBlog', 'Blog')}</a></li>
                <li><a href="#" className="hover:text-white transition-colors">{t('landing.footerCareers', 'Careers')}</a></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">{t('landing.footerLegal', 'Legal')}</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" className="hover:text-white transition-colors">{t('landing.footerPrivacy', 'Privacy')}</a></li>
                <li><a href="#" className="hover:text-white transition-colors">{t('landing.footerTerms', 'Terms')}</a></li>
              </ul>
            </div>
          </div>
          <div className="pt-8 border-t border-gray-800 text-sm text-center">
            <p>&copy; {new Date().getFullYear()} AI Agent Platform. {t('landing.footerRights', 'All rights reserved.')}</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
