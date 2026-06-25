/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#17202a',
        sea: '#087f8c',
        coral: '#f25f5c',
        mist: '#f6f8fb',
      },
    },
  },
  plugins: [],
};
