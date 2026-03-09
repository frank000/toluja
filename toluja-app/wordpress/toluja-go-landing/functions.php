<?php
/**
 * Toluja Go Landing theme functions.
 */

if (!defined('ABSPATH')) {
    exit;
}

function toluja_go_enqueue_assets(): void
{
    wp_enqueue_style(
        'toluja-go-style',
        get_stylesheet_uri(),
        [],
        wp_get_theme()->get('Version')
    );
}
add_action('wp_enqueue_scripts', 'toluja_go_enqueue_assets');

function toluja_go_register_lead_post_type(): void
{
    register_post_type('toluja_lead', [
        'labels' => [
            'name' => __('Leads Toluja', 'toluja-go-landing'),
            'singular_name' => __('Lead Toluja', 'toluja-go-landing'),
        ],
        'public' => false,
        'show_ui' => true,
        'show_in_menu' => true,
        'menu_icon' => 'dashicons-groups',
        'supports' => ['title'],
    ]);
}
add_action('init', 'toluja_go_register_lead_post_type');

function toluja_go_handle_trial_signup(): void
{
    if (!isset($_POST['toluja_nonce']) || !wp_verify_nonce(sanitize_text_field(wp_unslash($_POST['toluja_nonce'])), 'toluja_trial_signup')) {
        wp_safe_redirect(add_query_arg('cadastro', 'erro', wp_get_referer() ?: home_url('/')));
        exit;
    }

    $nome = isset($_POST['nome']) ? sanitize_text_field(wp_unslash($_POST['nome'])) : '';
    $whatsapp = isset($_POST['whatsapp']) ? sanitize_text_field(wp_unslash($_POST['whatsapp'])) : '';
    $empresa = isset($_POST['empresa']) ? sanitize_text_field(wp_unslash($_POST['empresa'])) : '';
    $email = isset($_POST['email']) ? sanitize_email(wp_unslash($_POST['email'])) : '';
    $canal = isset($_POST['canal']) ? sanitize_text_field(wp_unslash($_POST['canal'])) : '';
    $mensagem = isset($_POST['mensagem']) ? sanitize_textarea_field(wp_unslash($_POST['mensagem'])) : '';

    if (empty($nome) || empty($email) || !is_email($email)) {
        wp_safe_redirect(add_query_arg('cadastro', 'erro', wp_get_referer() ?: home_url('/')));
        exit;
    }

    $lead_id = wp_insert_post([
        'post_type' => 'toluja_lead',
        'post_title' => $nome . ' - ' . current_time('Y-m-d H:i'),
        'post_status' => 'publish',
    ]);

    if (!is_wp_error($lead_id) && $lead_id > 0) {
        update_post_meta($lead_id, 'nome', $nome);
        update_post_meta($lead_id, 'email', $email);
        update_post_meta($lead_id, 'whatsapp', $whatsapp);
        update_post_meta($lead_id, 'empresa', $empresa);
        update_post_meta($lead_id, 'canal', $canal);
        update_post_meta($lead_id, 'mensagem', $mensagem);

        $admin_email = get_option('admin_email');
        $assunto = 'Novo cadastro de teste gratuito - Toluja Go';
        $corpo = "Nome: {$nome}\n";
        $corpo .= "Email: {$email}\n";
        $corpo .= "WhatsApp: {$whatsapp}\n";
        $corpo .= "Empresa: {$empresa}\n";
        $corpo .= "Canal de venda: {$canal}\n";
        $corpo .= "Mensagem: {$mensagem}\n";

        wp_mail($admin_email, $assunto, $corpo);

        wp_safe_redirect(add_query_arg('cadastro', 'ok', wp_get_referer() ?: home_url('/')));
        exit;
    }

    wp_safe_redirect(add_query_arg('cadastro', 'erro', wp_get_referer() ?: home_url('/')));
    exit;
}
add_action('admin_post_nopriv_toluja_trial_signup', 'toluja_go_handle_trial_signup');
add_action('admin_post_toluja_trial_signup', 'toluja_go_handle_trial_signup');

function toluja_go_leads_columns(array $columns): array
{
    $new_columns = [];
    $new_columns['cb'] = $columns['cb'] ?? '<input type="checkbox" />';
    $new_columns['title'] = __('Lead', 'toluja-go-landing');
    $new_columns['email'] = __('E-mail', 'toluja-go-landing');
    $new_columns['whatsapp'] = __('WhatsApp', 'toluja-go-landing');
    $new_columns['empresa'] = __('Empresa', 'toluja-go-landing');
    $new_columns['date'] = __('Data', 'toluja-go-landing');

    return $new_columns;
}
add_filter('manage_toluja_lead_posts_columns', 'toluja_go_leads_columns');

function toluja_go_leads_custom_column(string $column, int $post_id): void
{
    if ('email' === $column) {
        echo esc_html((string) get_post_meta($post_id, 'email', true));
        return;
    }

    if ('whatsapp' === $column) {
        echo esc_html((string) get_post_meta($post_id, 'whatsapp', true));
        return;
    }

    if ('empresa' === $column) {
        echo esc_html((string) get_post_meta($post_id, 'empresa', true));
    }
}
add_action('manage_toluja_lead_posts_custom_column', 'toluja_go_leads_custom_column', 10, 2);

function toluja_go_add_lead_meta_box(): void
{
    add_meta_box(
        'toluja_lead_details',
        __('Dados do cadastro', 'toluja-go-landing'),
        'toluja_go_render_lead_meta_box',
        'toluja_lead',
        'normal',
        'high'
    );
}
add_action('add_meta_boxes_toluja_lead', 'toluja_go_add_lead_meta_box');

function toluja_go_render_lead_meta_box(\WP_Post $post): void
{
    $nome = (string) get_post_meta($post->ID, 'nome', true);
    $email = (string) get_post_meta($post->ID, 'email', true);
    $whatsapp = (string) get_post_meta($post->ID, 'whatsapp', true);
    $empresa = (string) get_post_meta($post->ID, 'empresa', true);
    $canal = (string) get_post_meta($post->ID, 'canal', true);
    $mensagem = (string) get_post_meta($post->ID, 'mensagem', true);
    ?>
    <table class="widefat striped" style="border: 0;">
        <tbody>
        <tr>
            <th style="width: 180px;"><?php esc_html_e('Nome', 'toluja-go-landing'); ?></th>
            <td><?php echo esc_html($nome); ?></td>
        </tr>
        <tr>
            <th><?php esc_html_e('E-mail', 'toluja-go-landing'); ?></th>
            <td>
                <?php if (!empty($email)) : ?>
                    <a href="mailto:<?php echo esc_attr($email); ?>"><?php echo esc_html($email); ?></a>
                <?php endif; ?>
            </td>
        </tr>
        <tr>
            <th><?php esc_html_e('WhatsApp', 'toluja-go-landing'); ?></th>
            <td><?php echo esc_html($whatsapp); ?></td>
        </tr>
        <tr>
            <th><?php esc_html_e('Empresa', 'toluja-go-landing'); ?></th>
            <td><?php echo esc_html($empresa); ?></td>
        </tr>
        <tr>
            <th><?php esc_html_e('Canal de venda', 'toluja-go-landing'); ?></th>
            <td><?php echo esc_html($canal); ?></td>
        </tr>
        <tr>
            <th><?php esc_html_e('Meta/Mensagem', 'toluja-go-landing'); ?></th>
            <td style="white-space: pre-wrap;"><?php echo esc_html($mensagem); ?></td>
        </tr>
        </tbody>
    </table>
    <?php
}

function toluja_go_lead_remove_editor(): void
{
    remove_post_type_support('toluja_lead', 'editor');
}
add_action('admin_init', 'toluja_go_lead_remove_editor');
