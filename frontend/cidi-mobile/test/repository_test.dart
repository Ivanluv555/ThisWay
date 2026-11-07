import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:cidi_mobile/data/cidi_repository.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  setUp(() => SharedPreferences.setMockInitialValues({}));
  test('local publish, collect, comment and search persist', () async {
    final repo = CidiRepository();
    await repo.initialize();
    await repo.loginLocal('旅行者');
    final post = await repo.publish(
      title: '测试记录',
      content: '安静的街道',
      categoryId: 1,
    );
    final id = '${post['id']}';
    await repo.toggle(id, 'collect');
    expect((await repo.collections()).any((p) => '${p['id']}' == id), isTrue);
    await repo.comment(id, '值得再来');
    expect((await repo.comments(id)).length, 1);
    await repo.rememberSearch('古镇');
    final restored = CidiRepository();
    await restored.initialize();
    expect((await restored.history()).contains('古镇'), isTrue);
    expect((await restored.posts()).any((p) => '${p['id']}' == id), isTrue);
    await restored.toggle(id, 'collect');
    expect(
      (await restored.collections()).any((p) => '${p['id']}' == id),
      isFalse,
    );
  });
  test('real mode rejects local identity writes', () async {
    final repo = CidiRepository();
    await repo.initialize();
    await repo.loginLocal('游客');
    await repo.configure(demo: false, baseUrl: 'https://example.com');
    await expectLater(
      repo.publish(title: 'test', content: '', categoryId: 1),
      throwsA(isA<StateError>()),
    );
  });
}
