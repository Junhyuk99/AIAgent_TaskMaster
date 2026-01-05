import { Link } from 'react-router-dom';

export default function HomePage() {
  const handleComingSoon = (e: React.MouseEvent<HTMLAnchorElement>) => {
    e.preventDefault();
    alert('추후 구현 예정입니다.');
  };

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
                로그인
              </Link>
              <Link
                to="/register"
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium"
              >
                시작하기
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
              지금 바로 사용 가능
            </div>
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-gray-900 dark:text-white mb-6 leading-tight">
              나만의 AI 에이전트를
              <br />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-primary-600 to-purple-600">
                코딩 없이 만들어보세요
              </span>
            </h1>
            <p className="text-lg sm:text-xl text-gray-600 dark:text-gray-300 mb-10 max-w-3xl mx-auto leading-relaxed">
              커스텀 함수와 지식베이스를 활용해 AI 에이전트를 생성하고 배포하세요.
              <br />
              다양한 LLM 제공자와 연동하여 강력한 대화형 경험을 구축할 수 있습니다.
            </p>
            <div className="flex flex-col sm:flex-row justify-center gap-4">
              <Link
                to="/register"
                className="px-8 py-4 bg-primary-600 text-white rounded-xl hover:bg-primary-700 transition-all font-semibold text-lg shadow-lg shadow-primary-500/25 hover:shadow-xl hover:shadow-primary-500/30 hover:-translate-y-0.5"
              >
                회원가입
              </Link>
              <Link
                to="/login"
                className="px-8 py-4 bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-700 transition-all font-semibold text-lg border border-gray-200 dark:border-gray-700"
              >
                로그인
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
              주요 기능
            </h2>
            <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
              강력한 AI 에이전트를 만들기 위한 모든 도구를 제공합니다
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
                커스텀 AI 에이전트
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                맞춤형 성격, 시스템 프롬프트, 동작 방식을 설정하여 나만의 에이전트를 만들 수 있습니다.
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
                함수 호출
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                커스텀 함수, API 연동, 코드 실행을 통해 에이전트의 기능을 확장할 수 있습니다.
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
                지식베이스
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                문서를 업로드하여 검색 가능한 지식베이스를 구축하고, 맥락에 맞는 답변을 제공합니다.
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
                다양한 LLM 지원
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                OpenAI, Anthropic, Google, Ollama 등 다양한 LLM 제공자와 연동할 수 있습니다.
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
                임베드 위젯
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                간단한 코드 스니펫으로 어떤 웹사이트에서든 AI 에이전트를 임베드할 수 있습니다.
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
                사용량 분석
              </h3>
              <p className="text-gray-600 dark:text-gray-400">
                대화 내역, 토큰 사용량, 에이전트 성능을 상세한 분석 리포트로 확인할 수 있습니다.
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
              활용 사례
            </h2>
            <p className="text-lg text-gray-600 dark:text-gray-400 max-w-2xl mx-auto">
              다양한 분야에서 AI 에이전트를 활용하여 업무를 혁신하세요
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <div className="bg-white dark:bg-gray-900 p-8 rounded-2xl shadow-sm hover:shadow-lg transition-shadow">
              <div className="flex items-center mb-4">
                <div className="w-12 h-12 bg-blue-100 dark:bg-blue-900 rounded-xl flex items-center justify-center mr-4">
                  <span className="text-2xl">💬</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
                  고객 지원
                </h3>
              </div>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                제품과 정책을 이해하는 AI 에이전트로 고객 문의를 자동화하세요.
              </p>
              <ul className="space-y-2 text-sm text-gray-500 dark:text-gray-400">
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 24시간 연중무휴 응대</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 즉각적인 답변 제공</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 일관된 서비스 품질</li>
              </ul>
            </div>

            <div className="bg-white dark:bg-gray-900 p-8 rounded-2xl shadow-sm hover:shadow-lg transition-shadow">
              <div className="flex items-center mb-4">
                <div className="w-12 h-12 bg-purple-100 dark:bg-purple-900 rounded-xl flex items-center justify-center mr-4">
                  <span className="text-2xl">📊</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
                  데이터 분석
                </h3>
              </div>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                자연어로 데이터를 조회하고 즉시 인사이트를 얻으세요.
              </p>
              <ul className="space-y-2 text-sm text-gray-500 dark:text-gray-400">
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 자연어 데이터 조회</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 맞춤형 시각화</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 자동 리포트 생성</li>
              </ul>
            </div>

            <div className="bg-white dark:bg-gray-900 p-8 rounded-2xl shadow-sm hover:shadow-lg transition-shadow">
              <div className="flex items-center mb-4">
                <div className="w-12 h-12 bg-green-100 dark:bg-green-900 rounded-xl flex items-center justify-center mr-4">
                  <span className="text-2xl">🎓</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
                  교육 및 트레이닝
                </h3>
              </div>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                AI 튜터와 학습 도우미로 맞춤형 학습 경험을 제공하세요.
              </p>
              <ul className="space-y-2 text-sm text-gray-500 dark:text-gray-400">
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 개인 맞춤 학습</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 인터랙티브 Q&A</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 학습 진도 추적</li>
              </ul>
            </div>

            <div className="bg-white dark:bg-gray-900 p-8 rounded-2xl shadow-sm hover:shadow-lg transition-shadow">
              <div className="flex items-center mb-4">
                <div className="w-12 h-12 bg-orange-100 dark:bg-orange-900 rounded-xl flex items-center justify-center mr-4">
                  <span className="text-2xl">🔧</span>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
                  개발자 도구
                </h3>
              </div>
              <p className="text-gray-600 dark:text-gray-400 mb-4">
                코드베이스와 문서를 이해하는 코딩 어시스턴트를 구축하세요.
              </p>
              <ul className="space-y-2 text-sm text-gray-500 dark:text-gray-400">
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 코드 설명</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 문서 검색</li>
                <li className="flex items-center"><span className="text-green-500 mr-2">✓</span> 버그 수정 지원</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8 bg-gradient-to-r from-primary-600 to-purple-600">
        <div className="container mx-auto max-w-4xl text-center">
          <h2 className="text-3xl sm:text-4xl font-bold text-white mb-6">
            지금 바로 AI 에이전트를 만들어보세요
          </h2>
          <p className="text-lg text-white/80 mb-10 max-w-2xl mx-auto">
            수많은 개발자와 기업들이 AI Agent 플랫폼으로 지능형 AI 경험을 구축하고 있습니다.
          </p>
          <div className="flex flex-col sm:flex-row justify-center gap-4">
            <Link
              to="/register"
              className="px-8 py-4 bg-white text-primary-600 rounded-xl hover:bg-gray-100 transition-all font-semibold text-lg shadow-lg"
            >
              무료로 시작하기
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
                비즈니스를 위한 지능형 AI 에이전트를 구축하세요.
              </p>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">제품</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" onClick={handleComingSoon} className="hover:text-white transition-colors">기능 소개</a></li>
                <li><a href="#" onClick={handleComingSoon} className="hover:text-white transition-colors">요금제</a></li>
                <li><a href="#" onClick={handleComingSoon} className="hover:text-white transition-colors">문서</a></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">회사</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" onClick={handleComingSoon} className="hover:text-white transition-colors">소개</a></li>
                <li><a href="#" onClick={handleComingSoon} className="hover:text-white transition-colors">블로그</a></li>
                <li><a href="#" onClick={handleComingSoon} className="hover:text-white transition-colors">채용</a></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-semibold mb-4">법적 고지</h4>
              <ul className="space-y-2 text-sm">
                <li><a href="#" onClick={handleComingSoon} className="hover:text-white transition-colors">개인정보처리방침</a></li>
                <li><a href="#" onClick={handleComingSoon} className="hover:text-white transition-colors">이용약관</a></li>
              </ul>
            </div>
          </div>
          <div className="pt-8 border-t border-gray-800 text-sm text-center">
            <p>&copy; {new Date().getFullYear()} AI Agent Platform. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
