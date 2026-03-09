<?php
/**
 * Front page template.
 */

if (!defined('ABSPATH')) {
    exit;
}

$cadastro_status = isset($_GET['cadastro']) ? sanitize_text_field(wp_unslash($_GET['cadastro'])) : '';
?>
<!DOCTYPE html>
<html <?php language_attributes(); ?>>
<head>
    <meta charset="<?php bloginfo('charset'); ?>">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <?php wp_head(); ?>
</head>
<body <?php body_class(); ?>>
<?php wp_body_open(); ?>
<header class="toluja-header">
    <div class="container toluja-nav">
        <a class="toluja-logo" href="#inicio" aria-label="Toluja Go">
            <img src="<?php echo esc_url(get_template_directory_uri() . '/assets/images/logo-transparente.png'); ?>" alt="Logo Toluja" class="toluja-logo-image">
            <span class="toluja-logo-text">
                <strong>Toluja Go</strong>
                <small>Tech + Consultoria IA</small>
            </span>
        </a>
        <nav class="toluja-nav-links" aria-label="Navegação principal">
            <a href="#beneficios">Benefícios</a>
            <a href="#planos">Planos</a>
            <a href="#faq">FAQ</a>
            <a href="#cadastro" class="btn btn-primary">Teste grátis</a>
        </nav>
    </div>
</header>

<main id="inicio">
    <section class="hero">
        <div class="container hero-wrap">
            <div>
                <span class="tag">Plataforma tech para escalar alcance e vendas</span>
                <h1>Venda em mais canais com o <br>Toluja Go e acelere seu faturamento</h1>
                <p>
                    O Toluja Go combina operação omnichannel com inteligência aplicada ao processo comercial.
                    Você centraliza pedidos, acelera atendimento e transforma dados em ação para vender mais com consistência.
                </p>
                <div class="hero-ctas">
                    <a href="#cadastro" class="btn btn-primary">Quero testar grátis</a>
                    <a href="#planos" class="btn btn-light">Ver planos</a>
                </div>
                <p class="hero-note">Plano grátis para quem vender até 1.500 na plataforma.</p>
            </div>
            <div class="hero-card">
                <img src="<?php echo esc_url(get_template_directory_uri() . '/assets/images/hero-dashboard.svg'); ?>" alt="Dashboard do Toluja Go com crescimento de vendas">
                <div class="metric-grid">
                    <article class="metric">
                        <strong>+38%</strong>
                        alcance médio
                    </article>
                    <article class="metric">
                        <strong>+27%</strong>
                        aumento de pedidos
                    </article>
                    <article class="metric">
                        <strong>-42%</strong>
                        tempo operacional
                    </article>
                </div>
            </div>
        </div>
    </section>

    <section id="beneficios" class="section">
        <div class="container">
            <h2>Por que o Toluja Go vende mais?</h2>
            <p class="section-subtitle">
                A plataforma foi desenhada para pequenos e médios negócios que querem crescer sem aumentar complexidade.
                Você ganha escala com automação, tecnologia orientada por IA e gestão clara das vendas.
            </p>

            <div class="benefits">
                <article class="benefit-card">
                    <div class="icon-pill">01</div>
                    <h3>Mais alcance em canais digitais</h3>
                    <p>Publique seus produtos e receba pedidos em mais pontos de contato sem duplicar trabalho.</p>
                </article>

                <article class="benefit-card">
                    <div class="icon-pill">02</div>
                    <h3>Conversão mais alta no atendimento</h3>
                    <p>Fluxo de venda mais rápido, com apoio de automações inteligentes e menos abandono de carrinho.</p>
                </article>

                <article class="benefit-card">
                    <div class="icon-pill">03</div>
                    <h3>Operação eficiente para escalar</h3>
                    <p>Controle de pedidos e rotina simplificada para sua equipe focar em vender mais todos os dias.</p>
                </article>
            </div>
        </div>
    </section>

    <section id="planos" class="section">
        <div class="container">
            <h2>Planos transparentes para crescer no seu ritmo</h2>
            <p class="section-subtitle">
                Comece grátis, valide resultado e evolua quando precisar de mais recursos para escalar suas vendas.
            </p>

            <div class="pricing">
                <article class="price-card featured">
                    <span class="badge">Melhor para começar</span>
                    <h3>Plano Grátis</h3>
                    <p class="price">R$ 0 <small>/mês</small></p>
                    <ul>
                        <li>Venda até 1.500 na plataforma</li>
                        <li>Cadastro de produtos e gestão de pedidos</li>
                        <li>Acesso aos recursos essenciais</li>
                    </ul>
                    <a href="#cadastro" class="btn btn-primary">Ativar teste</a>
                </article>

                <article class="price-card">
                    <h3>Plano Básico</h3>
                    <p class="price">R$ 59,90 <small>/mês</small></p>
                    <ul>
                        <li>Escala de vendas sem limite do plano grátis</li>
                        <li>Ferramentas para ampliar conversão</li>
                        <li>Suporte para operação diária</li>
                    </ul>
                    <a href="#cadastro" class="btn btn-light">Quero este plano</a>
                </article>

                <article class="price-card">
                    <h3>Plano Pro Custom</h3>
                    <p class="price">R$ 79,90 <small>/mês</small></p>
                    <ul>
                        <li>Customizações do ambiente</li>
                        <li>Experiência sem anúncios</li>
                        <li>Mais autonomia para sua marca</li>
                    </ul>
                    <a href="#cadastro" class="btn btn-light">Falar com consultor</a>
                </article>
            </div>
        </div>
    </section>

    <section class="section">
        <div class="container form-section" id="cadastro">
            <div>
                <h2>Cadastre-se para o teste gratuito</h2>
                <p>
                    Preencha o formulário e nosso time libera seu acesso para testar o Toluja Go com foco em aumentar alcance e vendas.
                    Sem compromisso.
                </p>
                <img src="<?php echo esc_url(get_template_directory_uri() . '/assets/images/growth-chart.svg'); ?>" alt="Ilustração de crescimento de vendas" style="width:100%; margin-top: 1rem; border-radius: 12px;">
            </div>

            <form class="lead-form" method="post" action="<?php echo esc_url(admin_url('admin-post.php')); ?>">
                <?php if ('ok' === $cadastro_status) : ?>
                    <div class="notice ok">Cadastro enviado com sucesso. Vamos entrar em contato em breve.</div>
                <?php elseif ('erro' === $cadastro_status) : ?>
                    <div class="notice error">Não foi possível enviar agora. Revise os campos e tente novamente.</div>
                <?php endif; ?>

                <input type="hidden" name="action" value="toluja_trial_signup">
                <?php wp_nonce_field('toluja_trial_signup', 'toluja_nonce'); ?>

                <label for="nome">Nome completo *</label>
                <input type="text" id="nome" name="nome" required>

                <label for="email">E-mail *</label>
                <input type="email" id="email" name="email" required>

                <label for="whatsapp">WhatsApp</label>
                <input type="text" id="whatsapp" name="whatsapp" placeholder="(11) 99999-9999">

                <label for="empresa">Nome da empresa</label>
                <input type="text" id="empresa" name="empresa">

                <label for="canal">Principal canal de venda hoje</label>
                <select id="canal" name="canal">
                    <option value="">Selecione</option>
                    <option>Instagram</option>
                    <option>WhatsApp</option>
                    <option>Loja física</option>
                    <option>Marketplace</option>
                    <option>Site próprio</option>
                </select>

                <label for="mensagem">Meta de vendas para os próximos 90 dias</label>
                <textarea id="mensagem" name="mensagem" placeholder="Ex: Quero dobrar os pedidos e organizar operação."></textarea>

                <button type="submit" class="btn btn-primary" style="width: 100%;">Começar teste gratuito</button>
            </form>
        </div>
    </section>

    <section id="faq" class="section">
        <div class="container">
            <h2>Dúvidas frequentes</h2>
            <div class="faq">
                <article class="faq-item">
                    <h3>O plano grátis tem limite?</h3>
                    <p>Sim. O uso é gratuito para quem vender até 1.500 na plataforma.</p>
                </article>
                <article class="faq-item">
                    <h3>Quanto custa depois?</h3>
                    <p>R$ 59,90 no plano básico ou R$ 79,90 no plano com customizações e sem anúncios.</p>
                </article>
                <article class="faq-item">
                    <h3>Preciso cartão para começar?</h3>
                    <p>Não. Você se cadastra para teste gratuito e valida o potencial da plataforma primeiro.</p>
                </article>
                <article class="faq-item">
                    <h3>Em quanto tempo começo a vender?</h3>
                    <p>Após configuração inicial, você já pode publicar produtos e começar a receber pedidos.</p>
                </article>
            </div>
        </div>
    </section>
</main>

<footer class="footer">
    <div class="container">© <?php echo esc_html(wp_date('Y')); ?> Toluja Go. Plataforma para ampliar alcance e aumentar vendas.</div>
</footer>

<?php wp_footer(); ?>
</body>
</html>
