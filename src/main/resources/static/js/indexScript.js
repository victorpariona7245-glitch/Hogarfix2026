// Initialize Lucide icons
    lucide.createIcons();

    // Mobile menu toggle
    function toggleMobileMenu() {
        const mobileMenu = document.querySelector('.mobile-menu');
        const menuIcon = document.getElementById('menu-icon');
        const closeIcon = document.getElementById('close-icon');

        mobileMenu.classList.toggle('active');
        menuIcon.classList.toggle('hidden');
        closeIcon.classList.toggle('hidden');
    }

    // Smooth scrolling for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });

                // Close mobile menu if open
                const mobileMenu = document.querySelector('.mobile-menu');
                if (mobileMenu.classList.contains('active')) {
                    toggleMobileMenu();
                }
            }
        });
    });

    // Contact form handling
    document.querySelectorAll('.btn-primary').forEach(button => {
        if (button.textContent.includes('Solicitar Servicio')) {
            button.addEventListener('click', function() {
                window.open('https://wa.me/51968459279?text=Hola, necesito solicitar un servicio técnico para mi hogar.', '_blank');
            });
        }
    });

    // Phone link handling
    document.querySelectorAll('a[href^="tel:"]').forEach(link => {
        link.addEventListener('click', function(e) {
            if (window.innerWidth > 768) {
                e.preventDefault();
                window.open('https://wa.me/51968459279', '_blank');
            }
        });
    });